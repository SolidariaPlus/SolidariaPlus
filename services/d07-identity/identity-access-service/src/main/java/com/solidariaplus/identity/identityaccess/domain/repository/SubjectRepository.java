package com.solidariaplus.identity.identityaccess.domain.repository;

import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.Optional;

public interface SubjectRepository {
    void save(Subject subject);

    Optional<Subject> findById(SubjectId sub);
}
