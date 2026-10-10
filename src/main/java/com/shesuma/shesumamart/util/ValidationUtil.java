package com.shesuma.shesumamart.util;

public final class ValidationUtil {
    private ValidationUtil() {}

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
