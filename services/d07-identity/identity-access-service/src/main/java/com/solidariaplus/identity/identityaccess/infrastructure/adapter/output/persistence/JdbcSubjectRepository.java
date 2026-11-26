package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.persistence;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.SubjectRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSubjectRepository implements SubjectRepository {
    private final JdbcClient jdbc;

    JdbcSubjectRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Subject subject) {
        jdbc.sql("INSERT INTO subjects (sub, member_id) VALUES (:sub, :memberId) ON CONFLICT (sub) DO NOTHING")
                .param("sub", subject.getSub().value())
                .param("memberId", subject.getMemberId().map(MemberId::value).orElse(null))
                .update();
    }

    @Override
    public Optional<Subject> findById(SubjectId sub) {
        return jdbc.sql("SELECT member_id FROM subjects WHERE sub = :sub")
                .param("sub", sub.value())
                .query((rs, row) -> {
                    UUID memberId = Columns.uuid(rs, "member_id");
                    return Subject.reconstitute(sub, memberId == null ? null : new MemberId(memberId));
                })
                .optional();
    }
}
