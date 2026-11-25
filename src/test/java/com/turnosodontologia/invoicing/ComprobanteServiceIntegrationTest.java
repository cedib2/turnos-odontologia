package com.turnosodontologia.invoicing;

import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.financiero.Comprobante;
import com.turnosodontologia.domain.financiero.Presupuesto;
import com.turnosodontologia.domain.financiero.Senia;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.ComprobanteRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.PresupuestoRepository;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AR-1..AR-14: ciclo completo de comprobantes sobre Testcontainers/PostgreSQL. */
@Import({RealAuditPublisherConfig.class, FacturacionTestConfig.class})
class ComprobanteServiceIntegrationTest extends PostgresFlywayTest {

    @Autowired
    private ComprobanteService servicio;

    @Autowired
    private ComprobanteRepository comprobantes;

    @Autowired
    private SeniaRepository senias;

    @Autowired
    private PacienteRepository pacientes;

    @Autowired
    private PresupuestoRepository presupuestos;

    @Autowired
    private AvisoRecepcionPort avisos;

    @Autowired
    private FacturacionTestConfig.FacturacionStub frente;

    @BeforeEach
    void reiniciar() {
        frente.reset();
        ((AvisoRecepcionSimulado) avisos).reset();
    }

    @Test
    void emiteUnaFacturaPorElMontoCobradoConPresupuestoYTurno() {
        Long presupuestoId = crearPresupuesto();
        Long seniaId = crearSenia(Senia.APROBADO, true, new BigDecimal("150.00"), presupuestoId, 42L);

        ComprobanteResponse respuesta = servicio.emitir(seniaId, ActorSimulado.recepcionista());

        assertEquals("factura", respuesta.tipo());
        assertEquals(Comprobante.EMITIDO, respuesta.estado());
        assertEquals(new BigDecimal("150.00"), respuesta.monto());
        assertEquals("ARS", respuesta.moneda());
        assertEquals(presupuestoId, respuesta.presupuestoId());
        assertEquals(42L, respuesta.turnoId());
        assertTrue(respuesta.concepto().contains(String.valueOf(presupuestoId)));
        assertTrue(FacturacionSimulada.esCaeDePrueba(respuesta.cae()));
        assertEquals(1, respuesta.intentos());
        assertEquals(1, comprobantes.count());
        assertAudit("factura.emitida", 1);
        assertEquals(1, contarIdempotencia(FacturacionIdempotencyStore.OPERACION_FACTURA));
    }

    @Test
    void emiteSinPresupuestoConConceptoGenerico() {
        Long seniaId = crearSenia(Senia.APROBADO, false, new BigDecimal("80.00"), null, null);

        ComprobanteResponse respuesta = servicio.emitir(seniaId, ActorSimulado.recepcionista());

        assertNull(respuesta.presupuestoId());
        assertNull(respuesta.turnoId());
        assertEquals("Seña odontológica", respuesta.concepto());
        assertEquals(Comprobante.EMITIDO, respuesta.estado());
        assertEquals(new BigDecimal("80.00"), respuesta.monto());
        assertAudit("factura.emitida", 1);
    }

    @Test
    void noFacturaUnaSeniaNoAprobadaNiElSaldoDelPresupuesto() {
        Long presupuestoId = crearPresupuesto();
        Long pendiente = crearSenia(Senia.PENDIENTE, false, new BigDecimal("150.00"), presupuestoId, null);

        assertThrows(IllegalStateException.class,
                () -> servicio.emitir(pendiente, ActorSimulado.recepcionista()));
        assertEquals(0, comprobantes.count());
        assertEquals(0, contarIdempotencia(FacturacionIdempotencyStore.OPERACION_FACTURA));
        assertAudit("factura.emitida", 0);

        Long aprobada = crearSenia(Senia.APROBADO, false, new BigDecimal("150.00"), presupuestoId, null);
        ComprobanteResponse respuesta = servicio.emitir(aprobada, ActorSimulado.recepcionista());
        assertEquals(new BigDecimal("150.00"), respuesta.monto());
        assertEquals(0, new BigDecimal("900.00").compareTo(
                presupuestos.findById(presupuestoId).orElseThrow().getTotal()));
    }

    @Test
    void reenvioYTransicionInvalidaNoRepitenEfectos() {
        Long seniaId = crearSenia(Senia.APROBADO, false, new BigDecimal("100.00"), null, null);
        ComprobanteResponse original = servicio.emitir(seniaId, ActorSimulado.recepcionista());

        ComprobanteResponse reenvio = servicio.emitir(seniaId, ActorSimulado.recepcionista());
        assertEquals(original.id(), reenvio.id());
        assertEquals(1, comprobantes.count());
        assertEquals(1, contarIdempotencia(FacturacionIdempotencyStore.OPERACION_FACTURA));
        assertAudit("factura.emitida", 1);

        assertThrows(IllegalStateException.class,
                () -> servicio.reintentar(original.id(), ActorSimulado.recepcionista()));
        assertAudit("factura.rechazada", 0);
        assertEquals(Comprobante.EMITIDO,
                comprobantes.findById(original.id()).orElseThrow().getEstado());
    }

    @Test
    void datoDelReceptorFaltanteQuedaPendienteYAvisaSinAuditoria() {
        Long seniaId = crearSenia(Senia.APROBADO, false, new BigDecimal("100.00"), 99999999L, null);

        ComprobanteResponse respuesta = servicio.emitir(seniaId, ActorSimulado.recepcionista());

        assertEquals(Comprobante.PENDIENTE, respuesta.estado());
        assertNull(respuesta.cae());
        assertEquals(0, respuesta.intentos());
        assertEquals(1, ((AvisoRecepcionSimulado) avisos).avisos().size());
        assertEquals(seniaId, ((AvisoRecepcionSimulado) avisos).avisos().get(0).seniaId());
        assertAudit("factura.emitida", 0);
        assertAudit("factura.rechazada", 0);
    }

    @Test
    void rechazoReintentoYErrorSaneadoSinSecretos() {
        Long seniaId = crearSenia(Senia.APROBADO, false, new BigDecimal("100.00"), null, null);
        frente.fallarSiguienteCon("Bearer APP_USR-token-super-secreto para https://arca.example");

        ComprobanteResponse rechazada = servicio.emitir(seniaId, ActorSimulado.recepcionista());
        assertEquals(Comprobante.RECHAZADO, rechazada.estado());
        assertNull(rechazada.cae());
        assertEquals(1, rechazada.intentos());
        String error = comprobantes.findById(rechazada.id()).orElseThrow().getUltimoError();
        assertNotNull(error);
        assertFalse(error.contains("APP_USR"));
        assertFalse(error.contains("arca.example"));
        assertAudit("factura.rechazada", 1);

        ComprobanteResponse reintentada = servicio.reintentar(
                rechazada.id(), ActorSimulado.recepcionista());
        assertEquals(Comprobante.EMITIDO, reintentada.estado());
        assertEquals(2, reintentada.intentos());
        assertNull(comprobantes.findById(rechazada.id()).orElseThrow().getUltimoError());
        assertEquals(1, contarIdempotencia(FacturacionIdempotencyStore.OPERACION_FACTURA));
        assertAudit("factura.emitida", 1);
        assertAudit("factura.rechazada", 1);
    }

    @Test
    void conciliaSoloConCaeMontoIgualYSenaAprobadaYConciliada() {
        Long conciliadaId = crearSenia(Senia.APROBADO, true, new BigDecimal("200.00"), null, null);
        ComprobanteResponse factura = servicio.emitir(conciliadaId, ActorSimulado.recepcionista());

        ConciliacionResponse resultado = servicio.conciliar(factura.id());
        assertTrue(resultado.conciliado());
        assertEquals("conciliada", resultado.motivo());
        assertTrue(comprobantes.findById(factura.id()).orElseThrow().isConciliado());
        assertAudit("factura.conciliada", 1);
        assertEquals("SYSTEM", modoActor("factura.conciliada"));
    }

    @Test
    void noConciliaSiLaSeniaAunNoEstaConciliadaNiSiFaltaElCae() {
        Long sinConciliar = crearSenia(Senia.APROBADO, false, new BigDecimal("200.00"), null, null);
        ComprobanteResponse factura = servicio.emitir(sinConciliar, ActorSimulado.recepcionista());

        ConciliacionResponse resultado = servicio.conciliar(factura.id());
        assertFalse(resultado.conciliado());
        assertEquals("senia_no_conciliada", resultado.motivo());
        assertFalse(comprobantes.findById(factura.id()).orElseThrow().isConciliado());
        assertAudit("factura.conciliada", 0);

        Long pendiente = crearSenia(Senia.APROBADO, true, new BigDecimal("200.00"), 99999998L, null);
        ComprobanteResponse sinCae = servicio.emitir(pendiente, ActorSimulado.recepcionista());
        ConciliacionResponse sinEmision = servicio.conciliar(sinCae.id());
        assertFalse(sinEmision.conciliado());
        assertEquals("comprobante_no_emitido", sinEmision.motivo());
        assertAudit("factura.conciliada", 0);
    }

    @Test
    void conciliacionConMontosDivergentesNoModificaLaSenia() {
        Long seniaId = crearSenia(Senia.APROBADO, true, new BigDecimal("200.00"), null, null);
        ComprobanteResponse factura = servicio.emitir(seniaId, ActorSimulado.recepcionista());
        jdbcTemplate.update("UPDATE comprobantes SET monto = ? WHERE id = ?",
                new BigDecimal("250.00"), factura.id());

        ConciliacionResponse resultado = servicio.conciliar(factura.id());
        assertFalse(resultado.conciliado());
        assertEquals("montos_divergentes", resultado.motivo());
        assertAudit("factura.conciliada", 0);
        Senia senia = senias.findById(seniaId).orElseThrow();
        assertEquals(new BigDecimal("200.00"), senia.getMonto());
        assertTrue(senia.isConciliado());
        assertEquals(Senia.APROBADO, senia.getEstado());
    }

    @Test
    void notaDeCreditoManualSobreSeniaReembolsadaAnulaLaFactura() {
        Long seniaId = crearSenia(Senia.APROBADO, true, new BigDecimal("300.00"), null, null);
        ComprobanteResponse factura = servicio.emitir(seniaId, ActorSimulado.recepcionista());
        Senia senia = senias.findById(seniaId).orElseThrow();
        senia.marcarReembolsada();
        senias.saveAndFlush(senia);
        assertEquals(1, comprobantes.count(), "aún no hay nota de crédito");
        assertTrue(comprobantes.findBySeniaIdAndTipo(seniaId, Comprobante.TIPO_NOTA_CREDITO).isEmpty());
        assertAudit("nota_credito.emitida", 0);

        ComprobanteResponse nota = servicio.notaCredito(factura.id(), ActorSimulado.recepcionista());

        assertEquals("nota_credito", nota.tipo());
        assertEquals(new BigDecimal("300.00"), nota.monto());
        assertEquals(Comprobante.EMITIDO, nota.estado());
        assertEquals(Comprobante.ANULADO,
                comprobantes.findById(factura.id()).orElseThrow().getEstado());
        assertAudit("factura.anulada", 1);
        assertAudit("nota_credito.emitida", 1);
        assertEquals("SIMULADO", modoActor("nota_credito.emitida"));
        assertEquals(1, contarIdempotencia(FacturacionIdempotencyStore.OPERACION_NOTA));

        ComprobanteResponse reenvio = servicio.notaCredito(factura.id(), ActorSimulado.recepcionista());
        assertEquals(nota.id(), reenvio.id());
        assertEquals(2, comprobantes.count());
        assertAudit("factura.anulada", 1);
        assertAudit("nota_credito.emitida", 1);
        assertEquals(1, contarIdempotencia(FacturacionIdempotencyStore.OPERACION_NOTA));
    }

    @Test
    void notaDeCreditoRechazaSeniaNoReembolsadaYSinDisparoAutomatico() {
        Long seniaId = crearSenia(Senia.APROBADO, true, new BigDecimal("300.00"), null, null);
        ComprobanteResponse factura = servicio.emitir(seniaId, ActorSimulado.recepcionista());

        assertThrows(IllegalStateException.class,
                () -> servicio.notaCredito(factura.id(), ActorSimulado.recepcionista()));
        assertEquals(Comprobante.EMITIDO,
                comprobantes.findById(factura.id()).orElseThrow().getEstado());
        assertEquals(1, comprobantes.count());
        assertAudit("nota_credito.emitida", 0);
        assertAudit("factura.anulada", 0);
    }

    @Test
    void metadataDeAuditoriaNoContieneCaeNiDatosDelReceptor() {
        Long seniaId = crearSenia(Senia.APROBADO, true, new BigDecimal("123.45"), crearPresupuesto(), 7L);
        ComprobanteResponse factura = servicio.emitir(seniaId, ActorSimulado.recepcionista());

        String metadata = metadata("factura.emitida");
        assertTrue(metadata.contains("comprobanteId"));
        assertTrue(metadata.contains("seniaId"));
        assertTrue(metadata.contains("presupuestoId"));
        assertTrue(metadata.contains("turnoId"));
        assertTrue(metadata.contains("\"moneda\"") && metadata.contains("ARS"),
                "metadata de auditoría con moneda ARS: " + metadata);
        assertTrue(metadata.contains("correlationId"));
        assertFalse(metadata.contains(factura.cae()), "el CAE no va a metadata");
        assertFalse(metadata.contains("DNI"));
        assertFalse(metadata.contains("APP_USR"));
        assertEquals("SIMULADO", modoActor("factura.emitida"));
    }

    @Test
    void cadaOperacionTieneSuPropioCorrelationId() {
        Long primera = crearSenia(Senia.APROBADO, false, new BigDecimal("100.00"), null, null);
        Long segunda = crearSenia(Senia.APROBADO, false, new BigDecimal("100.00"), null, null);
        servicio.emitir(primera, ActorSimulado.recepcionista());
        servicio.emitir(segunda, ActorSimulado.recepcionista());

        String primero = correlationId("factura.emitida", 0);
        String segundo = correlationId("factura.emitida", 1);
        assertNotNull(primero);
        assertNotNull(segundo);
        assertFalse(primero.equals(segundo));
    }

    private Long crearSenia(String estado, boolean conciliado, BigDecimal monto,
                            Long presupuestoId, Long turnoId) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Senia senia = new Senia(turnoId, presupuestoId, monto,
                "senia-c16-" + sufijo, "idem-c16-" + sufijo);
        if (!Senia.PENDIENTE.equals(estado)) {
            senia.aplicarEstado(Senia.APROBADO, Instant.now());
        }
        if (Senia.REEMBOLSADO.equals(estado)) {
            senia.marcarReembolsada();
        }
        if (conciliado) {
            senia.marcarConciliada();
        }
        return senias.saveAndFlush(senia).getId();
    }

    private Long crearPresupuesto() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Paciente paciente = pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                "C16-" + sufijo, "Nombre", "Apellido", "555"));
        Long id = presupuestos.saveAndFlush(new Presupuesto(paciente, null)).getId();
        jdbcTemplate.update("UPDATE presupuestos SET total = ? WHERE id = ?",
                new BigDecimal("900.00"), id);
        return id;
    }

    private void assertAudit(String tipo, int esperado) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log WHERE accion = ?", Integer.class, tipo);
        assertEquals(esperado, total, "filas de auditoría de " + tipo);
    }

    private int contarIdempotencia(String operacion) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public_idempotencia WHERE operacion = ?",
                Integer.class, operacion);
    }

    private String metadata(String tipo) {
        return jdbcTemplate.queryForObject(
                "SELECT metadata::text FROM auditoria_log WHERE accion = ? ORDER BY id LIMIT 1",
                String.class, tipo);
    }

    private String modoActor(String tipo) {
        return jdbcTemplate.queryForObject(
                "SELECT modo FROM auditoria_log WHERE accion = ? ORDER BY id LIMIT 1",
                String.class, tipo);
    }

    private String correlationId(String tipo, int offset) {
        return jdbcTemplate.queryForObject(
                "SELECT correlation_id FROM auditoria_log WHERE accion = ? ORDER BY id OFFSET ? LIMIT 1",
                String.class, tipo, offset);
    }
}
