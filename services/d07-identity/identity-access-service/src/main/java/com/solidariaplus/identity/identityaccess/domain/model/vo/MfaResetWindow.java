package com.solidariaplus.identity.identityaccess.domain.model.vo;

import java.time.Instant;
import java.util.Objects;

public record MfaResetWindow(Instant resetAt, SubjectId resetByBoardMember, Instant enrolmentDeadline) {
    public MfaResetWindow {
        Objects.requireNonNull(resetAt);
        Objects.requireNonNull(resetByBoardMember);
        Objects.requireNonNull(enrolmentDeadline);
        if (!enrolmentDeadline.isAfter(resetAt)) {
            throw new IllegalArgumentException("Enrolment deadline must be after resetAt");
        }
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(enrolmentDeadline);
    }
}
