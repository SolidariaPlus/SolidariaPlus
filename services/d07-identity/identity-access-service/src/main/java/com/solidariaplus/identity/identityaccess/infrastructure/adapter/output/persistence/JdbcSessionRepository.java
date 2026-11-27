package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.persistence;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Session;
import com.solidariaplus.identity.identityaccess.domain.model.entity.RefreshTokenFamily;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SessionId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.TokenHash;
import com.solidariaplus.identity.identityaccess.domain.repository.SessionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSessionRepository implements SessionRepository {
    private static final String SELECT = """
            SELECT s.session_id, s.sub, s.created_at, s.revoked_at, f.family_id, f.current_token_hash
            FROM sessions s JOIN refresh_token_families f ON f.session_id = s.session_id
            """;

    private final JdbcClient jdbc;

    JdbcSessionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Session session) {
        jdbc.sql("""
                        INSERT INTO sessions (session_id, sub, created_at, revoked_at)
                        VALUES (:sessionId, :sub, :createdAt, :revokedAt)
                        ON CONFLICT (session_id) DO UPDATE SET revoked_at = EXCLUDED.revoked_at
                        """)
                .param("sessionId", session.getSessionId().value())
                .param("sub", session.getSub().value())
                .param("createdAt", Columns.timestamp(session.getCreatedAt()))
                .param("revokedAt", Columns.timestamp(session.getRevokedAt()))
                .update();
        RefreshTokenFamily family = session.getTokenFamily();
        jdbc.sql("""
                        INSERT INTO refresh_token_families (family_id, session_id, current_token_hash)
                        VALUES (:familyId, :sessionId, :hash)
                        ON CONFLICT (family_id) DO UPDATE SET current_token_hash = EXCLUDED.current_token_hash
                        """)
                .param("familyId", family.getFamilyId())
                .param("sessionId", session.getSessionId().value())
                .param("hash", family.getCurrentTokenHash().sha256Hex())
                .update();
    }

    @Override
    public Optional<Session> findById(SessionId sessionId) {
        return jdbc.sql(SELECT + " WHERE s.session_id = :id")
                .param("id", sessionId.value())
                .query((rs, row) -> map(rs))
                .optional();
    }

    @Override
    public List<Session> findActiveBySubject(SubjectId sub) {
        return jdbc.sql(SELECT + " WHERE s.sub = :sub AND s.revoked_at IS NULL")
                .param("sub", sub.value())
                .query((rs, row) -> map(rs))
                .list();
    }

    private static Session map(ResultSet rs) throws SQLException {
        return Session.reconstitute(
                new SessionId(Columns.uuid(rs, "session_id")),
                new SubjectId(Columns.uuid(rs, "sub")),
                RefreshTokenFamily.reconstitute(
                        Columns.uuid(rs, "family_id"), new TokenHash(rs.getString("current_token_hash"))),
                Columns.instant(rs, "created_at"),
                Columns.instant(rs, "revoked_at"));
    }
}
