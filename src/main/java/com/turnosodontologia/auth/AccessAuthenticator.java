package com.turnosodontologia.auth;

import org.springframework.stereotype.Component;

@Component
public class AccessAuthenticator {

    private final JwtService jwt;
    private final AuthRevokedJtiRepository revocados;

    public AccessAuthenticator(JwtService jwt, AuthRevokedJtiRepository revocados) {
        this.jwt = jwt;
        this.revocados = revocados;
    }

    public JwtService.AccessClaims verificar(String access) {
        JwtService.AccessClaims claims = jwt.verificar(access);
        if (revocados.existsByJti(claims.jti())) {
            throw new InvalidAccessTokenException();
        }
        return claims;
    }
}
