package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.persistence;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.StaffProfile;
import com.solidariaplus.identity.identityaccess.domain.model.vo.DirectoryAccount;
import com.solidariaplus.identity.identityaccess.domain.model.vo.EncryptedTotpSecret;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MfaResetWindow;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.StaffProfileRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStaffProfileRepository implements StaffProfileRepository {
    private static final String SELECT = """
            SELECT sub, directory_account, roles, functions, totp_secret_enc,
                   mfa_reset_at, mfa_reset_by, mfa_enrolment_deadline
            FROM staff_profiles
            """;

    private final JdbcClient jdbc;

    JdbcStaffProfileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(StaffProfile profile) {
        Optional<MfaResetWindow> window = profile.getResetWindow();
        jdbc.sql("""
                        INSERT INTO staff_profiles (sub, directory_account, roles, functions, totp_secret_enc,
                            mfa_reset_at, mfa_reset_by, mfa_enrolment_deadline)
                        VALUES (:sub, :account, :roles, :functions, :totpSecretEnc, :resetAt, :resetBy, :deadline)
                        ON CONFLICT (sub) DO UPDATE SET roles = EXCLUDED.roles, functions = EXCLUDED.functions,
                            totp_secret_enc = EXCLUDED.totp_secret_enc, mfa_reset_at = EXCLUDED.mfa_reset_at,
                            mfa_reset_by = EXCLUDED.mfa_reset_by,
                            mfa_enrolment_deadline = EXCLUDED.mfa_enrolment_deadline
                        """)
                .param("sub", profile.getSub().value())
                .param("account", profile.getDirectoryAccount().username())
                .param("roles", Columns.textArray(profile.getRoles()))
                .param("functions", Columns.textArray(profile.getFunctions()))
                .param(
                        "totpSecretEnc",
                        profile.getTotpSecretEnc()
                                .map(EncryptedTotpSecret::ciphertext)
                                .orElse(null))
                .param("resetAt", Columns.timestamp(window.map(MfaResetWindow::resetAt)))
                .param(
                        "resetBy",
                        window.map(w -> w.resetByBoardMember().value()).orElse(null))
                .param("deadline", Columns.timestamp(window.map(MfaResetWindow::enrolmentDeadline)))
                .update();
    }

    @Override
    public Optional<StaffProfile> findBySub(SubjectId sub) {
        return find("sub", sub.value());
    }

    @Override
    public Optional<StaffProfile> findByDirectoryAccount(DirectoryAccount directoryAccount) {
        return find("directory_account", directoryAccount.username());
    }

    private Optional<StaffProfile> find(String column, Object value) {
        return jdbc.sql(SELECT + " WHERE " + column + " = :value")
                .param("value", value)
                .query((rs, row) -> map(rs))
                .optional();
    }

    private static StaffProfile map(ResultSet rs) throws SQLException {
        byte[] totpSecret = rs.getBytes("totp_secret_enc");
        return StaffProfile.reconstitute(
                new SubjectId(Columns.uuid(rs, "sub")),
                new DirectoryAccount(rs.getString("directory_account")),
                Columns.textSet(rs, "roles"),
                Columns.textSet(rs, "functions"),
                totpSecret == null ? null : new EncryptedTotpSecret(totpSecret),
                resetWindow(rs));
    }

    private static MfaResetWindow resetWindow(ResultSet rs) throws SQLException {
        Instant resetAt = Columns.instant(rs, "mfa_reset_at");
        UUID resetBy = Columns.uuid(rs, "mfa_reset_by");
        return resetAt == null
                ? null
                : new MfaResetWindow(resetAt, new SubjectId(resetBy), Columns.instant(rs, "mfa_enrolment_deadline"));
    }
}
