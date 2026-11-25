package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.waitlist.EntradaWaitlist;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checkpoint 1 y grupo 4: adjudicación atómica, depósito, conciliación y reembolso. */
class RellenoWaitlistIntegrationTest extends WaitlistFixture {

    private static final Instant INICIO = Instant.parse("2026-12-16T13:00:00Z");
    private static final Instant FIN = INICIO.plus(60, ChronoUnit.MINUTES);

    @Test
    void adjudicaHuecoLibreAlPrimerCandidatoSinDeposito() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        crearTratamiento("RELLENO-1", 60);
        Turno hueco = crearHuecoLiberado(profesional, sillon, "RELLENO-1", INICIO);
        EntradaWaitlist entrada = listaEspera.registrar(
                new AltaWaitlist(201L, profesional, sillon, INICIO, FIN, false));

        ResultadoRelleno resultado = relleno.adjudicarTurnoLiberado(hueco.getId());

        assertEquals(EntradaWaitlist.ADJUDICADA, resultado.estado());
        assertEquals("sin_deposito", resultado.detalle());
        assertEquals(1, ocupaciones.countByActivoTrue());
        Turno reasignado = turnos.findById(hueco.getId()).orElseThrow();
        assertEquals("confirmado", reasignado.getEstado());
        assertTrue(reasignado.isActivo());
        assertEquals(EntradaWaitlist.ADJUDICADA,
                entradas.findById(entrada.getId()).orElseThrow().getEstado());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class));
        assertTipo("waitlist.adjudicado", "SYSTEM");
    }

    @Test
    void huecoOcupadoSeRechazaSinEfectos() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        crearTratamiento("RELLENO-2", 60);
        Turno hueco = crearHuecoLiberado(profesional, sillon, "RELLENO-2", INICIO);
        listaEspera.registrar(new AltaWaitlist(202L, profesional, sillon, INICIO, FIN, false));
        crearTratamiento("OTRO-2", 30);
        creacion.crearTurno(crearPaciente(), profesional, sillon, "OTRO-2", INICIO,
                "confirmado", "otra-operacion");
        recorder.clear();

        assertThrows(IllegalStateException.class,
                () -> relleno.adjudicarTurnoLiberado(hueco.getId()));

        assertEquals(0, recorder.eventos().size());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public_idempotencia WHERE operacion = 'waitlist.adjudicar'",
                Integer.class));
    }

    @Test
    void reenvioNoCreaOtraOcupacionNiOtroEvento() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        crearTratamiento("RELLENO-3", 60);
        Turno hueco = crearHuecoLiberado(profesional, sillon, "RELLENO-3", INICIO);
        listaEspera.registrar(new AltaWaitlist(203L, profesional, sillon, INICIO, FIN, false));

        relleno.adjudicarTurnoLiberado(hueco.getId());
        assertThrows(IllegalStateException.class,
                () -> relleno.adjudicarTurnoLiberado(hueco.getId()));

        assertEquals(1, ocupaciones.countByActivoTrue());
        assertEquals(1, contarEventos("waitlist.adjudicado"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class));
    }

    @Test
    void depositoRequeridoCreaUnaSolaSeniaPendienteEnArs() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        crearTratamiento("RELLENO-4", 60);
        Turno hueco = crearHuecoLiberado(profesional, sillon, "RELLENO-4", INICIO);
        EntradaWaitlist entrada = listaEspera.registrar(
                new AltaWaitlist(204L, profesional, sillon, INICIO, FIN, true));

        ResultadoRelleno resultado = relleno.adjudicarTurnoLiberado(hueco.getId());

        assertEquals("con_deposito", resultado.detalle());
        Long seniaId = entradas.findById(entrada.getId()).orElseThrow().getSeniaId();
        assertNotNull(seniaId);
        assertEquals("pendiente", jdbcTemplate.queryForObject(
                "SELECT estado FROM senias WHERE id = ?", String.class, seniaId));
        assertEquals("ARS", jdbcTemplate.queryForObject(
                "SELECT moneda FROM senias WHERE id = ?", String.class, seniaId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class));
        assertTipo("waitlist.adjudicado", "SYSTEM");
    }

    @Test
    void conciliacionYReembolsoTotalLiberanElHueco() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        crearTratamiento("RELLENO-5", 60);
        Turno hueco = crearHuecoLiberado(profesional, sillon, "RELLENO-5", INICIO);
        EntradaWaitlist entrada = listaEspera.registrar(
                new AltaWaitlist(205L, profesional, sillon, INICIO, FIN, true));
        relleno.adjudicarTurnoLiberado(hueco.getId());
        Long seniaId = entradas.findById(entrada.getId()).orElseThrow().getSeniaId();
        jdbcTemplate.update("UPDATE senias SET estado = 'aprobado' WHERE id = ?", seniaId);

        assertTrue(relleno.conciliarDeposito(seniaId));
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT conciliado FROM senias WHERE id = ?", Boolean.class, seniaId));
        relleno.reembolsarDeposito(seniaId, hueco.getId(), ActorSimulado.recepcionista());

        assertEquals("reembolsado", jdbcTemplate.queryForObject(
                "SELECT estado FROM senias WHERE id = ?", String.class, seniaId));
        assertEquals(0, ocupaciones.countByActivoTrue());
        assertEquals("cancelado", turnos.findById(hueco.getId()).orElseThrow().getEstado());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name IN ('caja','asientos','arqueos')",
                Integer.class));
    }

    @Test
    void dosSolicitudesConLaMismaClaveCreanUnaSolaSeniaPendienteEnArs() {
        var solicitud = new com.turnosodontologia.depositpayments.SolicitudSenia(
                777L, null, BigDecimal.ONE, "waitlist-senia-clave-duplicada");

        var primera = senias.solicitar(solicitud, ActorSimulado.recepcionista());
        var segunda = senias.solicitar(solicitud, ActorSimulado.recepcionista());

        assertEquals(primera.id(), segunda.id());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class));
        assertEquals("pendiente", jdbcTemplate.queryForObject(
                "SELECT estado FROM senias WHERE id = ?", String.class, primera.id()));
        assertEquals("ARS", jdbcTemplate.queryForObject(
                "SELECT moneda FROM senias WHERE id = ?", String.class, primera.id()));
    }

    @Test
    void ofertaVencidaPasaAlSiguienteCandidato() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        crearTratamiento("RELLENO-7", 60);
        Turno hueco = crearHuecoLiberado(profesional, sillon, "RELLENO-7", INICIO);
        EntradaWaitlist primera = listaEspera.registrar(
                new AltaWaitlist(207L, profesional, sillon, INICIO, FIN, false));
        EntradaWaitlist segunda = listaEspera.registrar(
                new AltaWaitlist(208L, profesional, sillon, INICIO, FIN, false));
        jdbcTemplate.update("UPDATE lista_espera SET created_at = ? WHERE id = ?",
                Timestamp.from(WaitlistTestConfig.AHORA.minusSeconds(10)), primera.getId());
        jdbcTemplate.update("UPDATE lista_espera SET created_at = ? WHERE id = ?",
                Timestamp.from(WaitlistTestConfig.AHORA.minusSeconds(5)), segunda.getId());

        relleno.ofrecerTurnoLiberado(hueco.getId());
        jdbcTemplate.update("UPDATE lista_espera SET ofrecida_en = ? WHERE id = ?",
                Timestamp.from(WaitlistTestConfig.AHORA.minus(20, ChronoUnit.MINUTES)),
                primera.getId());

        ResultadoRelleno resultado = relleno.ofrecerTurnoLiberado(hueco.getId());

        assertEquals("ofrecida", resultado.estado());
        assertEquals(segunda.getId(), resultado.entradaId());
        assertEquals("expirada", entradas.findById(primera.getId()).orElseThrow().getEstado());
        assertEquals(0, ocupaciones.countByActivoTrue());
        assertEquals(2, notificaciones.ofertas().size());
    }

    @Test
    void ofrecerAlPrimerCandidatoNoAdjudicaYRegistraOferta() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        crearTratamiento("RELLENO-6", 60);
        Turno hueco = crearHuecoLiberado(profesional, sillon, "RELLENO-6", INICIO);
        EntradaWaitlist entrada = listaEspera.registrar(
                new AltaWaitlist(206L, profesional, sillon, INICIO, FIN, false));

        ResultadoRelleno resultado = relleno.ofrecerTurnoLiberado(hueco.getId());

        assertEquals("ofrecida", resultado.estado());
        assertEquals(entrada.getId(), resultado.entradaId());
        assertEquals(0, ocupaciones.countByActivoTrue());
        assertEquals(1, notificaciones.ofertas().size());
        assertTipo("waitlist.ofrecido", "SIMULADO");
    }

    private int contarEventos(String tipo) {
        return (int) recorder.eventos().stream().filter(evento -> tipo.equals(evento.tipo())).count();
    }

    private void assertTipo(String tipo, String modo) {
        List<AuditEvent> eventos = recorder.eventos().stream()
                .filter(evento -> tipo.equals(evento.tipo())).toList();
        assertEquals(1, eventos.size(), "falta el evento " + tipo);
        assertEquals(modo, eventos.get(0).actor().modo());
        assertFalse(eventos.get(0).metadata().containsKey("dni"));
        assertFalse(eventos.get(0).metadata().containsKey("email"));
        assertFalse(eventos.get(0).metadata().containsKey("secret"));
    }
}
