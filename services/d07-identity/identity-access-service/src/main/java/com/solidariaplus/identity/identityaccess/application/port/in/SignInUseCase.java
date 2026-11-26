package com.solidariaplus.identity.identityaccess.application.port.in;

import com.solidariaplus.identity.identityaccess.domain.model.vo.SubjectId;
import java.util.Set;

public interface SignInUseCase {
    AuthenticatedSubject signIn(String username, String password);

    record AuthenticatedSubject(SubjectId sub, Set<String> roles, Set<String> functions) {}
}
