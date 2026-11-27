package com.solidariaplus.identity.identityaccess.domain.model.vo;

import java.util.regex.Pattern;

public record MemberNumber(String value) {
    private static final Pattern FORMAT = Pattern.compile("^\\d{1,10}$");

    public MemberNumber {
        if (!isMemberNumber(value)) {
            throw new IllegalArgumentException("MemberNumber must have 1 to 10 digits");
        }
    }

    public static boolean isMemberNumber(String value) {
        return value != null && FORMAT.matcher(value).matches();
    }
}
