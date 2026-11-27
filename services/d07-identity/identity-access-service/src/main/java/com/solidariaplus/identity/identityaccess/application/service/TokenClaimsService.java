package com.solidariaplus.identity.identityaccess.application.service;

import com.solidariaplus.identity.identityaccess.application.port.in.TokenClaimsUseCase;
import com.solidariaplus.identity.identityaccess.domain.model.aggregate.Subject;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import com.solidariaplus.identity.identityaccess.domain.repository.StaffProfileRepository;
import com.solidariaplus.identity.identityaccess.domain.repository.SubjectRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TokenClaimsService implements TokenClaimsUseCase {
    private static final Set<String> MEMBER_ROLES = Set.of("MEMBER");
    private static final Claims NO_CLAIMS = new Claims(Set.of(), Set.of(), Optional.empty());

    private final SubjectRepository subjects;
    private final StaffProfileRepository staffProfiles;

    public TokenClaimsService(SubjectRepository subjects, StaffProfileRepository staffProfiles) {
        this.subjects = subjects;
        this.staffProfiles = staffProfiles;
    }

    @Override
    public Claims claimsOf(SubjectId sub) {
        Optional<MemberId> memberId = subjects.findById(sub).flatMap(Subject::getMemberId);
        if (memberId.isPresent()) {
            return new Claims(MEMBER_ROLES, Set.of(), memberId);
        }
        return staffProfiles
                .findBySub(sub)
                .map(profile -> new Claims(profile.getRoles(), profile.getFunctions(), Optional.empty()))
                .orElse(NO_CLAIMS);
    }
}
