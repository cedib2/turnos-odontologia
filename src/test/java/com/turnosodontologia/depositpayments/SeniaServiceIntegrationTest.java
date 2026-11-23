package com.turnosodontologia.depositpayments;

import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.domain.agenda.Consultorio;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.financiero.Presupuesto;
import com.turnosodontologia.domain.financiero.Senia;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.PresupuestoRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({RealAuditPublisherConfig.class, PagoTestConfig.class})
class SeniaServiceIntegrationTest extends PostgresFlywayTest {

    @Autowired
    private SeniaService servicio;

    @Autowired
    private SeniaRepository senias;

    @Autowired
    private PagoTestConfig.MercadoPagoStub mercadoPago;

    @Autowired
    private NotificacionPagoPort notificaciones;

    @Autowired
    private PacienteRepository pacientes;

    @Autowired
    private ProfesionalRepository profesionales;

    @Autowired
    private SillonRepository sillones;

    @Autowired
    private ConsultorioRepository consultorios;

    @Autowired
    private TratamientoRepository tratamientos;

    @Autowired
    private TurnoRepository turnos;

    @Autowired
    private PresupuestoRepository presupuestos;

    @BeforeEach
    void preparar() {
        mercadoPago.clear();
        ((NotificacionPagoSimulada) notificaciones).clear();
    }

    @Test
    void solicitudConPresupuestoYTurnoCreaUnaSolaSeniaYReenvioNoRepite() {
        Long turnoId = crearTurno();
        Long presupuestoId = crearPresupuesto(turnoId);
        SolicitudSenia solicitud = new SolicitudSenia(turnoId, presupuestoId,
                new BigDecimal("500.00"), "idem-021-01");

        SeniaResponse respuesta = servicio.solicitar(solicitud, ActorSimulado.recepcionista());

        assertEquals("pendiente", respuesta.estado());
        assertEquals("ARS", respuesta.moneda());
        assertTrue(respuesta.externalReference().startsWith("senia-"));
        assertTrue(respuesta.externalReference().matches(
                "senia-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertEquals("pref-" + respuesta.externalReference(), respuesta.preferenceId());
        assertEquals(1, senias.count());
        assertEquals(1, mercadoPago.preferencias().size());
        assertEquals("idem-021-01", mercadoPago.preferencias().get(0).idempotencyKey());
        assertAudit("pago.solicitado", 1);

        SeniaResponse reenvio = servicio.solicitar(solicitud, ActorSimulado.recepcionista());
        assertEquals(respuesta.id(), reenvio.id());
        assertEquals(1, senias.count());
        assertEquals(1, mercadoPago.preferencias().size());
        assertAudit("pago.solicitado", 1);
    }

    @Test
    void solicitudSinPresupuestoConservaElTurno() {
        Long turnoId = crearTurno();

        SeniaResponse respuesta = servicio.solicitar(
                new SolicitudSenia(turnoId, null, new BigDecimal("50.00"), "idem-021-02"),
                ActorSimulado.recepcionista());

        Senia guardada = senias.findById(respuesta.id()).orElseThrow();
        assertEquals(turnoId, guardada.getTurnoId());
        assertEquals(null, guardada.getPresupuestoId());
        assertEquals("ARS", guardada.getMoneda());
    }

    @Test
    void webhookAprobadoConciliayEmiteUnEventoPorOperacion() {
        Long turnoId = crearTurno();
        SeniaResponse seña = solicitar(turnoId, "idem-021-03");

        WebhookResult resultado = servicio.procesarWebhook(
                new WebhookSenia(seña.externalReference(), "approved", "pay-021-03", "ts-03"),
                "req-021-03", ActorSimulado.system());

        assertTrue(resultado.procesado());
        assertFalse(resultado.duplicado());
        assertTrue(resultado.conciliado());
        Senia actualizada = senias.findById(seña.id()).orElseThrow();
        assertEquals("aprobado", actualizada.getEstado());
        assertNotNull(actualizada.getFechaAprobacion());
        assertTrue(actualizada.isConciliado());
        assertAudit("pago.aprobado", 1);
        assertAudit("pago.conciliado", 1);
        assertAudit("pago.solicitado", 1);
        assertEquals(1, contarIdempotencia("req-021-03"));
    }

    @Test
    void webhookDuplicadoRespondeSinSegundoEfecto() {
        Long turnoId = crearTurno();
        SeniaResponse seña = solicitar(turnoId, "idem-021-04");
        WebhookSenia evento = new WebhookSenia(
                seña.externalReference(), "approved", "pay-021-04", "ts-04");

        servicio.procesarWebhook(evento, "req-021-04", ActorSimulado.system());
        WebhookResult duplicado = servicio.procesarWebhook(evento, "req-021-04", ActorSimulado.system());

        assertTrue(duplicado.duplicado());
        assertFalse(duplicado.procesado());
        assertEquals("aprobado", duplicado.estado());
        assertEquals(1, senias.count());
        assertAudit("pago.aprobado", 1);
        assertAudit("pago.conciliado", 1);
        assertEquals(1, contarIdempotencia("req-021-04"));
    }

    @Test
    void webhookRechazadoConservaElTurnoYAvisaPorElPuertoSimulado() {
        Long turnoId = crearTurno();
        SeniaResponse seña = solicitar(turnoId, "idem-021-05");

        WebhookResult resultado = servicio.procesarWebhook(
                new WebhookSenia(seña.externalReference(), "rejected", "pay-021-05", "ts-05"),
                "req-021-05", ActorSimulado.system());

        assertEquals("rechazado", resultado.estado());
        assertFalse(resultado.conciliado());
        assertEquals("confirmado", turnos.findById(turnoId).orElseThrow().getEstado());
        assertEquals(1, ((NotificacionPagoSimulada) notificaciones).solicitudes().size());
        assertEquals("pago.rechazado",
                ((NotificacionPagoSimulada) notificaciones).solicitudes().get(0).motivo());
        assertAudit("pago.rechazado", 1);
        assertAudit("pago.conciliado", 0);
    }

    @Test
    void contracargoHaceTerminalLaSenaYDescartaUnAprobadoTardio() {
        Long turnoId = crearTurno();
        SeniaResponse seña = solicitar(turnoId, "idem-021-06");

        WebhookResult cargo = servicio.procesarWebhook(
                new WebhookSenia(seña.externalReference(), "charged_back", "pay-021-06", "ts-06a"),
                "req-021-06a", ActorSimulado.system());
        WebhookResult tardio = servicio.procesarWebhook(
                new WebhookSenia(seña.externalReference(), "approved", "pay-021-06", "ts-06b"),
                "req-021-06b", ActorSimulado.system());

        assertEquals("reembolsado", cargo.estado());
        assertFalse(tardio.procesado());
        assertEquals("reembolsado", senias.findById(seña.id()).orElseThrow().getEstado());
        assertAudit("pago.reembolsado", 1);
        assertAudit("pago.aprobado", 0);
    }

    @Test
    void referenciaInexistenteNoConciliayNoCreaEntidades() {
        long turnosAntes = turnos.count();
        SeniaResponse seña = servicio.solicitar(
                new SolicitudSenia(999_999L, null, new BigDecimal("25.00"), "idem-021-07"),
                ActorSimulado.recepcionista());

        WebhookResult resultado = servicio.procesarWebhook(
                new WebhookSenia(seña.externalReference(), "approved", "pay-021-07", "ts-07"),
                "req-021-07", ActorSimulado.system());

        assertEquals("aprobado", resultado.estado());
        assertFalse(resultado.conciliado());
        assertFalse(senias.findById(seña.id()).orElseThrow().isConciliado());
        assertEquals(turnosAntes, turnos.count());
        assertAudit("pago.conciliado", 0);
    }

    @Test
    void reembolsoTotalLiberaElHuecoYSeAudita() {
        Long turnoId = crearTurno();
        SeniaResponse seña = solicitar(turnoId, "idem-021-08");
        servicio.procesarWebhook(new WebhookSenia(seña.externalReference(), "approved",
                "pay-021-08", "ts-08"), "req-021-08", ActorSimulado.system());

        RefundResponse reembolso = servicio.reembolsar(seña.id(), ActorSimulado.recepcionista());

        assertEquals("reembolsado", reembolso.estado());
        assertEquals("reembolsado", senias.findById(seña.id()).orElseThrow().getEstado());
        assertEquals("cancelado", turnos.findById(turnoId).orElseThrow().getEstado());
        assertFalse(turnos.findById(turnoId).orElseThrow().isActivo());
        assertEquals(1, mercadoPago.reembolsos().size());
        assertEquals(new BigDecimal("100.00"), mercadoPago.reembolsos().get(0).monto());
        assertAudit("pago.reembolsado", 1);
    }

    @Test
    void reembolsoDeSenaRechazadaSeRechazaSinLlamarAlProveedor() {
        Long turnoId = crearTurno();
        SeniaResponse seña = solicitar(turnoId, "idem-021-09");
        servicio.procesarWebhook(new WebhookSenia(seña.externalReference(), "rejected",
                "pay-021-09", "ts-09"), "req-021-09", ActorSimulado.system());

        assertThrows(IllegalStateException.class,
                () -> servicio.reembolsar(seña.id(), ActorSimulado.recepcionista()));
        assertTrue(mercadoPago.reembolsos().isEmpty());
        assertEquals("confirmado", turnos.findById(turnoId).orElseThrow().getEstado());
        assertAudit("pago.reembolsado", 0);
    }

    @Test
    void sinTablasDeCajaNiArqueoTrasLaMigracion() {
        Integer tablas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN ('caja', 'asientos', 'arqueo')", Integer.class);
        assertEquals(0, tablas);
    }

    private SeniaResponse solicitar(Long turnoId, String clave) {
        return servicio.solicitar(new SolicitudSenia(turnoId, null,
                new BigDecimal("100.00"), clave), ActorSimulado.recepcionista());
    }

    private void assertAudit(String tipo, int esperado) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log WHERE accion = ?", Integer.class, tipo);
        assertEquals(esperado, total, "filas de auditoría de " + tipo);
    }

    private int contarIdempotencia(String requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public_idempotencia WHERE operacion = 'pago.webhook'",
                Integer.class);
    }

    private Long crearTurno() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                "C15-" + sufijo, "Paciente", "C15", "555"));
        Profesional profesional = profesionales.saveAndFlush(new Profesional(
                "MP-" + sufijo, "Ana", "C15"));
        Consultorio consultorio = consultorios.findById(1L).orElseThrow();
        Sillon sillon = sillones.saveAndFlush(new Sillon("S-" + sufijo, consultorio));
        Tratamiento tratamiento = tratamientos.saveAndFlush(new Tratamiento(
                "T-" + sufijo, "Limpieza", 30));
        Instant inicio = Instant.parse("2027-02-15T10:00:00Z");
        Turno turno = new Turno(paciente, profesional, sillon, tratamiento, inicio,
                inicio.plus(30, ChronoUnit.MINUTES), 30, "confirmado", "recepcion", null);
        return turnos.saveAndFlush(turno).getId();
    }

    private Long crearPresupuesto(Long turnoId) {
        Turno turno = turnos.findById(turnoId).orElseThrow();
        Presupuesto presupuesto = new Presupuesto(turno.getPaciente(), turno);
        return presupuestos.saveAndFlush(presupuesto).getId();
    }
}
