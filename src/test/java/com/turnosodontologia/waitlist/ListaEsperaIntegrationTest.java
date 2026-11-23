package com.turnosodontologia.waitlist;

import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.waitlist.EntradaWaitlist;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Grupo 2: alta, ofrecimiento simulado y expiración determinista. */
class ListaEsperaIntegrationTest extends WaitlistFixture {

    private static final Instant INICIO = Instant.parse("2026-12-16T13:00:00Z");
    private static final Instant FIN = INICIO.plus(60, ChronoUnit.MINUTES);

    @Test
    void altaCreaEntradaSinTurnoNiOcupacionYEmiteWaitlistCreado() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();

        EntradaWaitlist entrada = listaEspera.registrar(
                new AltaWaitlist(101L, profesional, sillon, INICIO, FIN, true));

        assertNotNull(entrada.getId());
        assertEquals(EntradaWaitlist.ACTIVA, entrada.getEstado());
        assertEquals(0, turnos.count());
        assertEquals(0, ocupaciones.count());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class));
        assertEquals(1, recorder.eventos().size());
        AuditEvent evento = recorder.eventos().get(0);
        assertEquals("waitlist.creado", evento.tipo());
        assertEquals("lista_espera", evento.entidad());
        assertEquals("SIMULADO", evento.actor().modo());
        assertEquals("recepcionista", evento.actor().nombre());
        assertNotNull(evento.correlationId());
    }

    @Test
    void duplicadoSeRechazaSinEventos() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        AltaWaitlist alta = new AltaWaitlist(102L, profesional, sillon, INICIO, FIN, false);

        listaEspera.registrar(alta);
        assertThrows(IllegalArgumentException.class, () -> listaEspera.registrar(alta));

        assertEquals(1, entradas.count());
        assertEquals(1, recorder.eventos().size());
    }

    @Test
    void ofrecimientoUsaPuertoSimuladoYEmiteWaitlistOfrecido() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        EntradaWaitlist entrada = listaEspera.registrar(
                new AltaWaitlist(103L, profesional, sillon, INICIO, FIN, false));

        EntradaWaitlist ofrecida = listaEspera.ofrecer(entrada, 999L);

        assertEquals(EntradaWaitlist.OFRECIDA, ofrecida.getEstado());
        assertEquals(1, notificaciones.ofertas().size());
        assertEquals(ofrecida.getId(), notificaciones.ofertas().get(0).entradaId());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class));
        List<AuditEvent> eventos = recorder.eventos();
        assertEquals(2, eventos.size());
        assertEquals("waitlist.ofrecido", eventos.get(1).tipo());
        assertEquals("SIMULADO", eventos.get(1).actor().modo());
    }

    @Test
    void ofertaVencidaSegunRelojFijoExpiraYNoCreaSenia() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        EntradaWaitlist entrada = listaEspera.registrar(
                new AltaWaitlist(104L, profesional, sillon, INICIO, FIN, true));
        EntradaWaitlist ofrecida = listaEspera.ofrecer(entrada, 999L);
        jdbcTemplate.update("UPDATE lista_espera SET ofrecida_en = ? WHERE id = ?",
                java.sql.Timestamp.from(WaitlistTestConfig.AHORA.minus(20, ChronoUnit.MINUTES)),
                ofrecida.getId());

        EntradaWaitlist expirada = listaEspera.expirar(
                entradas.findById(ofrecida.getId()).orElseThrow());

        assertNotNull(expirada);
        assertEquals(EntradaWaitlist.EXPIRADA, expirada.getEstado());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class));
        List<AuditEvent> eventos = recorder.eventos();
        assertEquals("waitlist.expirado", eventos.get(eventos.size() - 1).tipo());
        assertEquals("SYSTEM", eventos.get(eventos.size() - 1).actor().modo());
    }

    @Test
    void ofertaDentroDelPlazoNoExpira() {
        Long profesional = crearProfesional();
        Long sillon = crearSillon();
        EntradaWaitlist entrada = listaEspera.registrar(
                new AltaWaitlist(105L, profesional, sillon, INICIO, FIN, true));
        EntradaWaitlist ofrecida = listaEspera.ofrecer(entrada, 999L);

        assertEquals(null, listaEspera.expirar(ofrecida));
        assertEquals(EntradaWaitlist.OFRECIDA,
                entradas.findById(ofrecida.getId()).orElseThrow().getEstado());
    }
}
