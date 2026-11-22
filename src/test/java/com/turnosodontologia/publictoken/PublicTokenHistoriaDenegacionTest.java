package com.turnosodontologia.publictoken;

import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicTokenHistoriaDenegacionTest {

    @Test
    void historiaSinIdentidadVerificadaDeniegaYPublicaEventoSimuladoSinPersistir() {
        Instant ahora = Instant.parse("2026-11-21T12:00:00Z");
        PublicTokenSigner firmador = new PublicTokenSigner(PublicTokenTestSupport.SEMILLA,
                PublicTokenTestSupport.EMISOR);
        PublicTokenTestSupport.FakeMetadataStore metadatos = new PublicTokenTestSupport.FakeMetadataStore();
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();
        PublicTokenService servicio = new PublicTokenService(firmador, metadatos, eventos,
                PublicTokenTestSupport.reloj(ahora));

        PublicTokenClaims claims = PublicTokenTestSupport.claims("jti-historia-001",
                PublicTokenScope.HISTORIA, ahora, null, "paciente-42");
        String token = servicio.emitir(claims);

        PublicTokenService.Validacion resultado = servicio.validar(token);

        assertFalse(resultado.permitido());
        assertEquals("identidad_no_verificada", resultado.motivo());
        assertNull(resultado.claims());
        assertEquals(1, eventos.eventos().size());
        AuditEvent evento = eventos.eventos().get(0);
        assertEquals("SIMULADO", evento.actor().modo());
        assertEquals("paciente", evento.actor().nombre());
        assertEquals("HISTORIA", evento.metadata().get("scope"));
        assertEquals("jti-historia-001", evento.metadata().get("jti"));
        assertFalse(evento.metadata().containsValue(token));
        assertEquals(1, metadatos.registros().size());

        PublicTokenClaims conIdentidad = new PublicTokenClaims("jti-historia-002",
                PublicTokenTestSupport.EMISOR, "paciente-42", "paciente-42",
                PublicTokenScope.HISTORIA, ahora.getEpochSecond(),
                ahora.getEpochSecond() + PublicTokenScope.HISTORIA.vigenciaSegundos(), null, true);
        assertThrows(IllegalArgumentException.class, () -> servicio.emitir(conIdentidad));
    }
}
