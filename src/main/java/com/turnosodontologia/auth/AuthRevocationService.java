package com.turnosodontologia.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AuthRevocationService {

    private final AuthSessionRepository sesiones;
    private final AuthRefreshTokenRepository refreshes;
    private final AuthRevokedJtiRepository jtis;

    public AuthRevocationService(AuthSessionRepository sesiones,
                                 AuthRefreshTokenRepository refreshes,
                                 AuthRevokedJtiRepository jtis) {
        this.sesiones = sesiones;
        this.refreshes = refreshes;
        this.jtis = jtis;
    }

    public void revocarSesion(String jti, Instant ahora) {
        AuthSession sesion = sesiones.findByJti(jti).orElseThrow(InvalidAccessTokenException::new);
        sesion.revocar(ahora);
        for (AuthRefreshToken token : refreshes.findBySesionId(sesion.getId())) {
            token.revocar(ahora);
        }
        if (!jtis.existsByJti(jti)) {
            jtis.saveAndFlush(new AuthRevokedJti(jti, sesion, ahora));
        }
    }
}
