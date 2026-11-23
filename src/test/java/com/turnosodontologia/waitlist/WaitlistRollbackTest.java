package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.agenda.Turno;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** W-7: un fallo de auditoría revierte idempotencia, ocupación, seña y estado. */
@Import({WaitlistFailingAuditConfig.class, WaitlistTestConfig.class})
class WaitlistRollbackTest extends WaitlistFixture {

    private static final Instant INICIO = Instant.parse("2026-12-16T13:00:00Z");

    @Test
    void falloDeAuditoriaRevierteTodosLosEfectosDeLaAdjudicacion() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        crearTratamiento("ROLLBACK-1", 60);
        Turno hueco = crearHuecoLiberado(profesional, sillon, "ROLLBACK-1", INICIO);
        var entrada = listaEspera.registrar(new com.turnosodontologia.waitlist.AltaWaitlist(
                301L, profesional, sillon, INICIO, INICIO.plus(60, ChronoUnit.MINUTES), true));

        assertThrows(IllegalStateException.class,
                () -> relleno.adjudicarTurnoLiberado(hueco.getId()));

        assertEquals(0, ocupaciones.countByActivoTrue());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public_idempotencia WHERE operacion = 'waitlist.adjudicar'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log WHERE accion = 'waitlist.adjudicado'",
                Integer.class));
        assertEquals("cancelado", turnos.findById(hueco.getId()).orElseThrow().getEstado());
        assertEquals("activa", entradas.findById(entrada.getId()).orElseThrow().getEstado());
    }
}
