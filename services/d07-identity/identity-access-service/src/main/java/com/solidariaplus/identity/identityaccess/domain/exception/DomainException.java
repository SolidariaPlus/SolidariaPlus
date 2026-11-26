package com.solidariaplus.identity.identityaccess.domain.exception;

public final class DomainException extends RuntimeException {
    private final ErrorCode code;

    public DomainException(ErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
