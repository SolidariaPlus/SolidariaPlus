package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.persistence;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.MemberAccount;
import com.solidariaplus.identity.identityaccess.domain.model.entity.ActivationCode;
import com.solidariaplus.identity.identityaccess.domain.model.vo.Argon2Hash;
import com.solidariaplus.identity.identityaccess.domain.model.vo.EncryptedTotpSecret;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberAccountStatus;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.MemberAccountRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMemberAccountRepository implements MemberAccountRepository {
    private static final String SELECT = """
            SELECT a.member_id, a.sub, a.member_number, a.password_hash, a.totp_secret_enc, a.status,
                   c.code_id, c.code_hash, c.created_at, c.expires_at, c.used
            FROM member_accounts a
            LEFT JOIN LATERAL (
                SELECT * FROM activation_codes WHERE member_id = a.member_id ORDER BY created_at DESC LIMIT 1
            ) c ON true
            """;

    private final JdbcClient jdbc;

    JdbcMemberAccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(MemberAccount account) {
        jdbc.sql("""
                        INSERT INTO member_accounts
                            (member_id, sub, member_number, password_hash, totp_secret_enc, status)
                        VALUES (:memberId, :sub, :memberNumber, :passwordHash, :totpSecretEnc, :status)
                        ON CONFLICT (member_id) DO UPDATE SET password_hash = EXCLUDED.password_hash,
                            totp_secret_enc = EXCLUDED.totp_secret_enc, status = EXCLUDED.status
                        """)
                .param("memberId", account.getMemberId().value())
                .param("sub", account.getSub().value())
                .param("memberNumber", account.getMemberNumber().value())
                .param(
                        "passwordHash",
                        account.getPasswordHash().map(Argon2Hash::hash).orElse(null))
                .param(
                        "totpSecretEnc",
                        account.getTotpSecretEnc()
                                .map(EncryptedTotpSecret::ciphertext)
                                .orElse(null))
                .param("status", account.getStatus().name())
                .update();
        account.getActiveCode().ifPresent(code -> saveCode(account.getMemberId(), code));
    }

    @Override
    public Optional<MemberAccount> findById(MemberId memberId) {
        return find("a.member_id", memberId.value());
    }

    @Override
    public Optional<MemberAccount> findByMemberNumber(MemberNumber memberNumber) {
        return find("a.member_number", memberNumber.value());
    }

    private Optional<MemberAccount> find(String column, Object value) {
        return jdbc.sql(SELECT + " WHERE " + column + " = :value")
                .param("value", value)
                .query((rs, row) -> map(rs))
                .optional();
    }

    private void saveCode(MemberId memberId, ActivationCode code) {
        jdbc.sql("""
                        INSERT INTO activation_codes (code_id, member_id, code_hash, created_at, expires_at, used)
                        VALUES (:codeId, :memberId, :codeHash, :createdAt, :expiresAt, :used)
                        ON CONFLICT (code_id) DO UPDATE SET used = EXCLUDED.used
                        """)
                .param("codeId", code.getCodeId())
                .param("memberId", memberId.value())
                .param("codeHash", code.getCodeHash().hash())
                .param("createdAt", Columns.timestamp(code.getCreatedAt()))
                .param("expiresAt", Columns.timestamp(code.getExpiresAt()))
                .param("used", code.isUsed())
                .update();
    }

    private static MemberAccount map(ResultSet rs) throws SQLException {
        String passwordHash = rs.getString("password_hash");
        byte[] totpSecret = rs.getBytes("totp_secret_enc");
        return MemberAccount.reconstitute(
                new MemberId(Columns.uuid(rs, "member_id")),
                new SubjectId(Columns.uuid(rs, "sub")),
                new MemberNumber(rs.getString("member_number")),
                passwordHash == null ? null : new Argon2Hash(passwordHash),
                totpSecret == null ? null : new EncryptedTotpSecret(totpSecret),
                activationCode(rs),
                MemberAccountStatus.valueOf(rs.getString("status")));
    }

    private static ActivationCode activationCode(ResultSet rs) throws SQLException {
        UUID codeId = Columns.uuid(rs, "code_id");
        return codeId == null
                ? null
                : ActivationCode.reconstitute(
                        codeId,
                        new Argon2Hash(rs.getString("code_hash")),
                        Columns.instant(rs, "created_at"),
                        Columns.instant(rs, "expires_at"),
                        rs.getBoolean("used"));
    }
}
