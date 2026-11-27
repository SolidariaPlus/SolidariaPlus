package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SecondFactor;
import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;

public interface SecondFactorUseCase {
    SecondFactor requirementOf(SubjectId sub);

    String newSecret();

    void enrol(SubjectId sub, String secret, String code);

    void verify(SubjectId sub, String code);
}
