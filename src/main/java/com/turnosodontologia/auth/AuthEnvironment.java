package com.turnosodontologia.auth;

import java.util.List;

public final class AuthEnvironment {

    public static final String JWT_SECRET = "${AUTH_JWT_SECRET}";
    public static final String JWT_ISSUER = "${AUTH_JWT_ISSUER:turnos-odontologia}";
    public static final String SESSION_COOKIE = "${AUTH_SESSION_COOKIE:auth_session}";
    public static final String CSRF_COOKIE = "${AUTH_CSRF_COOKIE:auth_csrf}";
    public static final List<String> VARIABLES = List.of(
            "AUTH_JWT_SECRET", "AUTH_JWT_ISSUER", "AUTH_SESSION_COOKIE", "AUTH_CSRF_COOKIE");

    private AuthEnvironment() {
    }
}
