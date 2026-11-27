package com.solidariaplus.identity.identityaccess.domain.service;

import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import java.util.regex.Pattern;

public final class PasswordPolicy {
    private static final Pattern RULE = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{12,}$");

    private PasswordPolicy() {}

    public static void validate(String password) {
        if (password == null || !RULE.matcher(password).matches()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
