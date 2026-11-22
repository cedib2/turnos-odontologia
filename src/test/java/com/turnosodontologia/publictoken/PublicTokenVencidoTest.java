package com.turnosodontologia.publictoken;

import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicTokenVencidoTest {

    @Test
    void tokenVencidoSeRechazaSinEfectosYHabilitaPedirNuevoEnlace() {
        Instant ahora = Instant.parse("2026-11-21T12:00:00Z");
        Instant emitido = ahora.minusSeconds(7200);
        PublicTokenSigner firmador = new PublicTokenSigner(PublicTokenTestSupport.SEMILLA,
                PublicTokenTestSupport.EMISOR);
        PublicTokenTestSupport.FakeMetadataStore metadatos = new PublicTokenTestSupport.FakeMetadataStore();
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();
        PublicTokenService servicio = new PublicTokenService(firmador, metadatos, eventos,
                PublicTokenTestSupport.reloj(ahora));

        PublicTokenClaims claims = new PublicTokenClaims("jti-vencido-001",
                PublicTokenTestSupport.EMISOR, "paciente-7", "paciente-7",
                PublicTokenScope.RESERVAR, emitido.getEpochSecond(),
                emitido.getEpochSecond() + PublicTokenScope.RESERVAR.vigenciaSegundos(), null, false);
        String token = servicio.emitir(claims);

        PublicTokenService.Validacion resultado = servicio.validar(token);

        assertFalse(resultado.permitido());
        assertEquals("token_vencido", resultado.motivo());
        assertTrue(resultado.requiereNuevoEnlace());
        assertNull(resultado.claims());
        assertTrue(metadatos.estaDisponible("jti-vencido-001"));
        assertTrue(eventos.eventos().isEmpty());
    }
}
