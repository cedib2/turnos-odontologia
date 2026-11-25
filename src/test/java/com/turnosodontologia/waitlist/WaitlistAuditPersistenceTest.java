package com.turnosodontologia.waitlist;

import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.support.PostgresFlywayTest;
import com.turnosodontologia.TestJpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** W-10: los seis tipos del catálogo se persisten con metadata cerrada y correlation única. */
@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
@Import({WaitlistTestConfig.class, RealAuditPublisherConfig.class})
class WaitlistAuditPersistenceTest extends PostgresFlywayTest {

    @Autowired
    private WaitlistAuditPublisher publisher;

    @Test
    void persisteUnFilaPorCadaUnoDeLosSeisTiposAprobados() {
        publicar("waitlist.creado", "lista_espera", ActorSimulado.recepcionista());
        publicar("waitlist.ofrecido", "lista_espera", ActorSimulado.recepcionista());
        publicar("waitlist.adjudicado", "lista_espera", ActorSimulado.system());
        publicar("waitlist.expirado", "lista_espera", ActorSimulado.system());
        publicar("turno.inasistencia", "turno", ActorSimulado.recepcionista());
        publicar("metrica.publicada", "metrica", ActorSimulado.system());

        for (String tipo : List.of("waitlist.creado", "waitlist.ofrecido", "waitlist.adjudicado",
                "waitlist.expirado", "turno.inasistencia", "metrica.publicada")) {
            assertEquals(1, contar(tipo), "falta una fila para " + tipo);
        }
        String metadata = jdbcTemplate.queryForObject(
                "SELECT metadata::text FROM auditoria_log WHERE accion = 'metrica.publicada'",
                String.class);
        for (String clave : List.of("entradaId", "turnoId", "seniaId", "metricaId", "sillonId",
                "periodo", "ocupacion", "ausentismo", "metodologia", "motivo", "correlationId")) {
            assertTrue(metadata.contains(clave), "falta la clave permitida " + clave);
        }
        for (String prohibida : List.of("dni", "telefono", "email", "diagnostico",
                "historia_clinica", "password", "secret", "access_token")) {
            assertFalse(metadata.contains("\"" + prohibida + "\""), "clave prohibida " + prohibida);
        }
        Long correlaciones = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT correlation_id) FROM auditoria_log", Long.class);
        assertEquals(6L, correlaciones);
        assertEquals(6L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auditoria_log", Long.class));
    }

    private void publicar(String tipo, String entidad, ActorSimulado actor) {
        String correlation = UUID.randomUUID().toString();
        publisher.publicar(tipo, entidad, 1L, actor, "fixture",
                WaitlistAuditMetadata.de(correlation)
                        .entrada(1L).turno(2L).senia(3L).metrica(4L).sillon(5L)
                        .periodo("2026-12").ocupacion("70.0000").ausentismo("15.0000")
                        .metodologia("ocupacion=100*ocupados/disponibles; fuente=turnos,bloqueos")
                        .motivo("fixture"));
    }

    private int contar(String tipo) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log WHERE accion = ?", Integer.class, tipo);
    }
}
