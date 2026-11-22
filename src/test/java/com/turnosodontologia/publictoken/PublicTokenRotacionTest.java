package com.turnosodontologia.publictoken;

import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicTokenRotacionTest extends PostgresFlywayTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void jtiUsadoSeRechazaYSoloGestionarPuedeRotarConTransicionAtomica() {
        Instant ahora = Instant.parse("2026-11-21T12:00:00Z");
        PublicTokenSigner firmador = new PublicTokenSigner(PublicTokenTestSupport.SEMILLA,
                PublicTokenTestSupport.EMISOR);
        JdbcPublicTokenMetadataStore metadatos = new JdbcPublicTokenMetadataStore(dataSource);
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();
        PublicTokenService servicio = new PublicTokenService(firmador, metadatos, eventos,
                PublicTokenTestSupport.reloj(ahora));

        PublicTokenClaims usado = PublicTokenTestSupport.claims("jti-usado-001",
                PublicTokenScope.GESTIONAR, ahora, "turno-70", "paciente-1");
        String tokenUsado = servicio.emitir(usado);
        assertTrue(metadatos.marcarUsado("jti-usado-001"));
        PublicTokenService.Validacion rechazado = servicio.validar(tokenUsado);
        assertFalse(rechazado.permitido());
        assertEquals("jti_reutilizado", rechazado.motivo());

        PublicTokenClaims reservar = PublicTokenTestSupport.claims("jti-reservar-001",
                PublicTokenScope.RESERVAR, ahora, null, "paciente-2");
        String tokenReservar = servicio.emitir(reservar);
        PublicTokenService.Rotacion noRotada = servicio.rotar(tokenReservar);
        assertFalse(noRotada.permitido());
        assertEquals("rotacion_no_permitida", noRotada.motivo());
        assertTrue(metadatos.estaDisponible("jti-reservar-001"));

        PublicTokenClaims gestionar = PublicTokenTestSupport.claims("jti-gestionar-001",
                PublicTokenScope.GESTIONAR, ahora, "turno-77", "paciente-3");
        String tokenGestionar = servicio.emitir(gestionar);
        PublicTokenService.Rotacion rotada = servicio.rotar(tokenGestionar);

        assertTrue(rotada.permitido());
        assertNotEquals(tokenGestionar, rotada.token());
        assertFalse(metadatos.estaDisponible("jti-gestionar-001"));
        PublicTokenService.Validacion nueva = servicio.validar(rotada.token());
        assertTrue(nueva.permitido());
        assertNotEquals("jti-gestionar-001", nueva.claims().jti());
        assertEquals("turno-77", nueva.claims().turnoId());
        assertEquals(4, contarFilas("public_token_jti"));
    }

    private int contarFilas(String tabla) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }
}
