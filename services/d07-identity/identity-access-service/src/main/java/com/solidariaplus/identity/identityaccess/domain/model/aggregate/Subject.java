package com.solidariaplus.identity.identityaccess.domain.model.aggregate;

import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.Objects;
import java.util.Optional;

public final class Subject {
    private final SubjectId sub;
    private final MemberId memberId;

    private Subject(SubjectId sub, MemberId memberId) {
        this.sub = Objects.requireNonNull(sub, "sub cannot be null");
        this.memberId = memberId;
    }

    public static Subject forMember(MemberId memberId) {
        return new Subject(SubjectId.generate(), Objects.requireNonNull(memberId, "memberId cannot be null"));
    }

    public static Subject forStaff() {
        return new Subject(SubjectId.generate(), null);
    }

    public static Subject reconstitute(SubjectId sub, MemberId memberId) {
        return new Subject(sub, memberId);
    }

    public SubjectId getSub() {
        return sub;
    }

    public Optional<MemberId> getMemberId() {
        return Optional.ofNullable(memberId);
    }
}
