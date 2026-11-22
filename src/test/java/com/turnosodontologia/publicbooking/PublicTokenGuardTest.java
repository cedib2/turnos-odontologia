package com.turnosodontologia.publicbooking;

import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenScope;
import com.turnosodontologia.publictoken.PublicTokenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicTokenGuardTest {

    private final PublicTokenService tokens = mock(PublicTokenService.class);
    private final PublicTokenGuard guardia = new PublicTokenGuard(tokens);

    @Test
    void turnoAjenoSeRechazaAntesDeCualquierEscritura() {
        when(tokens.validar(anyString())).thenReturn(new PublicTokenService.Validacion(
                true, "ok", claims(PublicTokenScope.GESTIONAR, "12"), false));

        PublicApiException excepcion = assertThrows(PublicApiException.class,
                () -> guardia.validar("token-gestionar", PublicTokenScope.GESTIONAR, 11L));

        assertEquals("turno_ajeno", excepcion.motivo());
    }

    @Test
    void pacienteIdNoNumericoSeRechazaComoTokenInvalido() {
        when(tokens.validar(anyString())).thenReturn(new PublicTokenService.Validacion(
                true, "ok", claimsConPacienteInvalido(), false));

        PublicApiException excepcion = assertThrows(PublicApiException.class,
                () -> guardia.validar("token-reservar", PublicTokenScope.RESERVAR));

        assertEquals("identificador_invalido", excepcion.motivo());
    }

    @Test
    void scopeReservarNoSirveParaGestionar() {
        when(tokens.validar(anyString())).thenReturn(new PublicTokenService.Validacion(
                true, "ok", claims(PublicTokenScope.RESERVAR, null), false));

        PublicApiException excepcion = assertThrows(PublicApiException.class,
                () -> guardia.validar("token-reservar", PublicTokenScope.GESTIONAR, 11L));

        assertEquals("scope_no_permitido", excepcion.motivo());
    }

    private static PublicTokenClaims claims(PublicTokenScope scope, String turnoId) {
        return new PublicTokenClaims("jti", "turnos-odontologia-public", "paciente-42", "42",
                scope, 1000L, 5000L, turnoId, false);
    }

    private static PublicTokenClaims claimsConPacienteInvalido() {
        return new PublicTokenClaims("jti", "turnos-odontologia-public", "paciente",
                "no-numerico", PublicTokenScope.RESERVAR, 1000L, 5000L, null, false);
    }
}
