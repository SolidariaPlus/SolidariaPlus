package com.solidariaplus.identity.identityaccess.application.port.in;

public interface ActivateAccountUseCase {
    void activate(Command command);

    record Command(String memberNumber, String activationCode, String newPassword) {}
}
