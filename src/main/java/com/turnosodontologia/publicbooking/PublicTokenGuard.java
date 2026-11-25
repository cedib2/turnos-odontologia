package com.turnosodontologia.publicbooking;

import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenScope;
import com.turnosodontologia.publictoken.PublicTokenService;
import org.springframework.http.HttpStatus;

public final class PublicTokenGuard {

    private final PublicTokenService tokens;

    public PublicTokenGuard(PublicTokenService tokens) {
        this.tokens = tokens;
    }

    public Validacion validar(String valor, PublicTokenScope esperado) {
        return validar(valor, esperado, null);
    }

    public Validacion validar(String valor, PublicTokenScope esperado, Long turnoIdRuta) {
        if (valor == null || valor.isBlank()) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "token_requerido");
        }
        PublicTokenService.Validacion validacion = tokens.validar(valor);
        if (!validacion.permitido()) {
            if (validacion.requiereNuevoEnlace()) {
                throw PublicApiException.vencido(validacion.motivo());
            }
            throw new PublicApiException(HttpStatus.BAD_REQUEST, validacion.motivo());
        }
        PublicTokenClaims claims = validacion.claims();
        if (claims.scope() != esperado) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "scope_no_permitido");
        }
        if (claims.pacienteId() != null) {
            comoLong(claims.pacienteId());
        }
        if (esperado == PublicTokenScope.GESTIONAR) {
            Long turnoDelToken = comoLong(claims.turnoId());
            if (turnoIdRuta != null && !turnoDelToken.equals(turnoIdRuta)) {
                throw new PublicApiException(HttpStatus.BAD_REQUEST, "turno_ajeno");
            }
        }
        return new Validacion(claims);
    }

    private Long comoLong(String valor) {
        try {
            return Long.valueOf(valor);
        } catch (RuntimeException ex) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "identificador_invalido");
        }
    }

    public record Validacion(PublicTokenClaims claims) {
    }
}
