package com.turnosodontologia.invoicing;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.financiero.Comprobante;
import com.turnosodontologia.domain.financiero.Presupuesto;
import com.turnosodontologia.domain.financiero.Senia;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.ComprobanteRepository;
import com.turnosodontologia.infrastructure.persistence.PresupuestoRepository;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de C-16: emisión, reintentos, conciliación y nota de crédito. */
@Service
public class ComprobanteService {

    private final ComprobanteRepository comprobantes;
    private final SeniaRepository senias;
    private final PresupuestoRepository presupuestos;
    private final AuditEventPublisher auditoria;
    private final FacturacionPort facturacion;
    private final AvisoRecepcionPort avisos;
    private final FacturacionIdempotencyStore idempotencia;
    private final String letra;
    private final String puntoVenta;

    public ComprobanteService(ComprobanteRepository comprobantes, SeniaRepository senias,
                              PresupuestoRepository presupuestos, AuditEventPublisher auditoria,
                              FacturacionPort facturacion, AvisoRecepcionPort avisos,
                              FacturacionIdempotencyStore idempotencia,
                              @Value("${arca.comprobante.letra:C}") String letra,
                              @Value("${arca.comprobante.punto-venta:0001}") String puntoVenta) {
        this.comprobantes = comprobantes;
        this.senias = senias;
        this.presupuestos = presupuestos;
        this.auditoria = auditoria;
        this.facturacion = facturacion;
        this.avisos = avisos;
        this.idempotencia = idempotencia;
        this.letra = letra;
        this.puntoVenta = puntoVenta;
    }

    @Transactional
    public ComprobanteResponse emitir(Long seniaId, ActorSimulado actor) {
        if (seniaId == null) {
            throw new IllegalArgumentException("El id de la seña es obligatorio");
        }
        Optional<Comprobante> previo = comprobantes.findBySeniaIdAndTipo(
                seniaId, Comprobante.TIPO_FACTURA);
        if (previo.isPresent()) {
            return ComprobanteResponse.de(previo.get());
        }

        Senia senia = senias.findById(seniaId)
                .orElseThrow(() -> new NoSuchElementException("Seña inexistente"));
        if (!Senia.APROBADO.equals(senia.getEstado())) {
            throw new IllegalStateException("Solo se factura una seña aprobada");
        }

        Comprobante comprobante = new Comprobante(senia.getId(), senia.getPresupuestoId(),
                senia.getTurnoId(), senia.getMonto(), concepto(senia));
        if (!idempotencia.registrar(FacturacionIdempotencyStore.OPERACION_FACTURA,
                claveFactura(seniaId))) {
            return comprobantes.findBySeniaIdAndTipo(seniaId, Comprobante.TIPO_FACTURA)
                    .map(ComprobanteResponse::de)
                    .orElseThrow(() -> new IllegalStateException("Emisión ya registrada"));
        }
        comprobantes.saveAndFlush(comprobante);

        if (!datosReceptorDisponibles(senia)) {
            avisos.solicitar(seniaId, "datos_receptor_faltantes");
            return ComprobanteResponse.de(comprobante);
        }

        intentar(comprobante, actor, "emision", true);
        return ComprobanteResponse.de(comprobante);
    }

    @Transactional
    public ComprobanteResponse reintentar(Long comprobanteId, ActorSimulado actor) {
        Comprobante comprobante = obtener(comprobanteId);
        if (!comprobante.prepararReintento()) {
            throw new IllegalStateException("Solo se puede reintentar un comprobante rechazado");
        }
        comprobantes.saveAndFlush(comprobante);
        intentar(comprobante, actor, "reintento", false);
        return ComprobanteResponse.de(comprobante);
    }

    @Transactional
    public ConciliacionResponse conciliar(Long comprobanteId) {
        Comprobante comprobante = obtener(comprobanteId);
        if (!comprobante.esElegibleParaConciliacion()) {
            return new ConciliacionResponse(comprobanteId, false, "comprobante_no_emitido");
        }
        Senia senia = senias.findById(comprobante.getSeniaId()).orElse(null);
        if (senia == null) {
            return new ConciliacionResponse(comprobanteId, false, "senia_inexistente");
        }
        if (!Senia.APROBADO.equals(senia.getEstado()) || !senia.isConciliado()) {
            return new ConciliacionResponse(comprobanteId, false, "senia_no_conciliada");
        }
        if (senia.getMonto().compareTo(comprobante.getMonto()) != 0) {
            return new ConciliacionResponse(comprobanteId, false, "montos_divergentes");
        }
        comprobante.marcarConciliada();
        comprobantes.saveAndFlush(comprobante);
        publicar("factura.conciliada", comprobante, ActorSimulado.system(), "conciliacion");
        return new ConciliacionResponse(comprobanteId, true, "conciliada");
    }

    @Transactional
    public ComprobanteResponse notaCredito(Long facturaId, ActorSimulado actor) {
        Comprobante factura = obtener(facturaId);
        // La idempotencia se resuelve antes de validar el estado: la factura ya
        // está anulada cuando se reenvía una nota de crédito emitida (AR-9/AR-11).
        Optional<Comprobante> previa = comprobantes.findBySeniaIdAndTipo(
                factura.getSeniaId(), Comprobante.TIPO_NOTA_CREDITO);
        if (previa.isPresent()) {
            return ComprobanteResponse.de(previa.get());
        }
        if (!factura.esFacturaEmitida()) {
            throw new IllegalStateException("Solo se puede anular una factura emitida");
        }
        Senia senia = senias.findById(factura.getSeniaId())
                .orElseThrow(() -> new NoSuchElementException("Seña inexistente"));
        if (!Senia.REEMBOLSADO.equals(senia.getEstado())) {
            throw new IllegalStateException("La nota de crédito exige una seña reembolsada");
        }

        Comprobante nota = new Comprobante(factura.getSeniaId(), factura.getPresupuestoId(),
                factura.getTurnoId(), Comprobante.TIPO_NOTA_CREDITO, factura.getMonto(),
                "Nota de crédito por reembolso de seña");
        if (!idempotencia.registrar(FacturacionIdempotencyStore.OPERACION_NOTA,
                claveNota(facturaId))) {
            return comprobantes.findBySeniaIdAndTipo(factura.getSeniaId(),
                            Comprobante.TIPO_NOTA_CREDITO)
                    .map(ComprobanteResponse::de)
                    .orElseThrow(() -> new IllegalStateException("Nota ya registrada"));
        }
        comprobantes.saveAndFlush(nota);
        intentar(nota, actor, "nota de credito", true);

        if (Comprobante.EMITIDO.equals(nota.getEstado())) {
            factura.anular();
            comprobantes.saveAndFlush(factura);
            publicar("factura.anulada", factura, actor, "nota de credito");
        }
        return ComprobanteResponse.de(nota);
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse consultar(Long comprobanteId) {
        return ComprobanteResponse.de(obtener(comprobanteId));
    }

    private Comprobante obtener(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del comprobante es obligatorio");
        }
        return comprobantes.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Comprobante inexistente"));
    }

    private void intentar(Comprobante comprobante, ActorSimulado actor, String motivo,
                          boolean contarIntento) {
        if (contarIntento) {
            comprobante.contarIntento();
        }
        SolicitudFacturacion solicitud = new SolicitudFacturacion(comprobante.getTipo(),
                referencia(comprobante), comprobante.getConcepto(), comprobante.getMonto(),
                comprobante.getMoneda(), letra, puntoVenta);
        FacturacionResultado resultado = facturacion.emitir(solicitud);
        if (resultado.exitoso()) {
            if (!comprobante.emitir(resultado.cae(), Instant.now())) {
                throw new IllegalStateException("Transición de emisión no permitida");
            }
            publicar(tipoExito(comprobante), comprobante, actor, motivo);
        } else {
            if (!comprobante.rechazar(ErrorSaneador.sanear(resultado.codigoError()))) {
                throw new IllegalStateException("Transición de rechazo no permitida");
            }
            publicar("factura.rechazada", comprobante, actor, motivo);
        }
        comprobantes.saveAndFlush(comprobante);
    }

    private String tipoExito(Comprobante comprobante) {
        return Comprobante.TIPO_NOTA_CREDITO.equals(comprobante.getTipo())
                ? "nota_credito.emitida" : "factura.emitida";
    }

    private String referencia(Comprobante comprobante) {
        return Comprobante.TIPO_NOTA_CREDITO.equals(comprobante.getTipo())
                ? "nota-" + comprobante.getId() : "senia-" + comprobante.getSeniaId();
    }

    private void publicar(String tipo, Comprobante comprobante, ActorSimulado actor,
                          String motivo) {
        String correlationId = UUID.randomUUID().toString();
        auditoria.publish(new AuditEvent(tipo, "comprobante", comprobante.getId(), actor,
                Instant.now(), motivo,
                FacturacionAuditMetadata.de(comprobante, motivo, correlationId), correlationId));
    }

    private String concepto(Senia senia) {
        if (senia.getPresupuestoId() != null) {
            return "Seña odontológica — presupuesto #" + senia.getPresupuestoId();
        }
        return "Seña odontológica";
    }

    private boolean datosReceptorDisponibles(Senia senia) {
        if (senia.getPresupuestoId() == null) {
            return true;
        }
        Presupuesto presupuesto = presupuestos.findById(senia.getPresupuestoId()).orElse(null);
        if (presupuesto == null || presupuesto.getPaciente() == null) {
            return false;
        }
        Paciente paciente = presupuesto.getPaciente();
        return texto(paciente.getNombre()) && texto(paciente.getApellido())
                && paciente.getDocumentoTipo() != null && texto(paciente.getDocumentoNumero());
    }

    private boolean texto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String claveFactura(Long seniaId) {
        return String.valueOf(seniaId);
    }

    private String claveNota(Long facturaId) {
        return "nota-" + facturaId;
    }
}
