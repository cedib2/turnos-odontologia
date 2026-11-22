package com.turnosodontologia.publictoken;

import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicTokenForjadoTest {

    @Test
    void firmaAjenaYEmisorDistintoSeRechazanConElMismoMotivoSinEfectos() {
        Instant ahora = Instant.parse("2026-11-21T12:00:00Z");
        PublicTokenSigner firmador = new PublicTokenSigner(PublicTokenTestSupport.SEMILLA,
                PublicTokenTestSupport.EMISOR);
        PublicTokenTestSupport.FakeMetadataStore metadatos = new PublicTokenTestSupport.FakeMetadataStore();
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();
        PublicTokenService servicio = new PublicTokenService(firmador, metadatos, eventos,
                PublicTokenTestSupport.reloj(ahora));

        PublicTokenClaims valido = PublicTokenTestSupport.claims("jti-valido-001",
                PublicTokenScope.RESERVAR, ahora, null, "paciente-9");
        PublicTokenClaims conFirmaAjena = PublicTokenTestSupport.claims("jti-forjado-001",
                PublicTokenScope.RESERVAR, ahora, null, "paciente-9");
        PublicTokenClaims conEmisorAjeno = new PublicTokenClaims("jti-forjado-002",
                "emisor-distinto", "paciente-9", "paciente-9", PublicTokenScope.RESERVAR,
                ahora.getEpochSecond(),
                ahora.getEpochSecond() + PublicTokenScope.RESERVAR.vigenciaSegundos(), null, false);
        String tokenValido = servicio.emitir(valido);
        String tokenFirmadoAjeno = new PublicTokenSigner("otra-semilla-de-prueba-2026",
                PublicTokenTestSupport.EMISOR).emitir(conFirmaAjena);
        String tokenEmisorAjeno = new PublicTokenSigner(PublicTokenTestSupport.SEMILLA,
                "emisor-distinto").emitir(conEmisorAjeno);

        assertTrue(servicio.validar(tokenValido).permitido());
        PublicTokenService.Validacion porFirma = servicio.validar(tokenFirmadoAjeno);
        PublicTokenService.Validacion porEmisor = servicio.validar(tokenEmisorAjeno);

        assertFalse(porFirma.permitido());
        assertFalse(porEmisor.permitido());
        assertEquals("token_no_valido", porFirma.motivo());
        assertEquals(porFirma.motivo(), porEmisor.motivo());
        assertNull(porFirma.claims());
        assertNull(porEmisor.claims());
        assertEquals(1, metadatos.registros().size());
        assertTrue(eventos.eventos().isEmpty());
    }
}
