package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;

public interface SignInUseCase {
    SubjectId signIn(String username, String password);
}
