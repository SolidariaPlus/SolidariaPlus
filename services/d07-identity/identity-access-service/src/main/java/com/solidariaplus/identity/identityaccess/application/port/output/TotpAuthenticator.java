package com.solidariaplus.identity.identityaccess.application.port.output;

import java.time.Instant;

public interface TotpAuthenticator {
    String newSecret();

    boolean verify(String secret, String code, Instant now);
}
