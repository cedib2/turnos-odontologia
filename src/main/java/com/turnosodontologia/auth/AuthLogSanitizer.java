package com.turnosodontologia.auth;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AuthLogSanitizer {

    private static final String MASK = "***";
    private static final String SENSITIVE_KEY =
            "(?i).*(password|hash|jwt|token|secret|clave|authorization|cookie).*";

    private AuthLogSanitizer() {
    }

    public static Map<String, Object> mask(Map<String, Object> values) {
        Map<String, Object> masked = new LinkedHashMap<>();
        values.forEach((key, value) -> masked.put(key, isSensitive(key) ? MASK : value));
        return masked;
    }

    private static boolean isSensitive(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
        return normalized.matches(SENSITIVE_KEY);
    }
}
