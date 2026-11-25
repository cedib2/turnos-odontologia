package com.turnosodontologia.depositpayments;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.financiero.Senia;
import com.turnosodontologia.infrastructure.persistence.PresupuestoRepository;
import com.turnosodontologia.infrastructure.persistence.SeniaRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de señas y pagos de C-15. */
@Service
public class SeniaService {

    private final SeniaRepository senias;
    private final TurnoRepository turnos;
    private final PresupuestoRepository presupuestos;
    private final AuditEventPublisher auditoria;
    private final MercadoPagoPort mercadoPago;
    private final NotificacionPagoPort notificaciones;
    private final MercadoPagoIdempotencyStore idempotencia;

    public SeniaService(SeniaRepository senias, TurnoRepository turnos,
                        PresupuestoRepository presupuestos, AuditEventPublisher auditoria,
                        MercadoPagoPort mercadoPago, NotificacionPagoPort notificaciones,
                        MercadoPagoIdempotencyStore idempotencia) {
        this.senias = senias;
        this.turnos = turnos;
        this.presupuestos = presupuestos;
        this.auditoria = auditoria;
        this.mercadoPago = mercadoPago;
        this.notificaciones = notificaciones;
        this.idempotencia = idempotencia;
    }

    @Transactional
    public SeniaResponse solicitar(SolicitudSenia solicitud, ActorSimulado actor) {
        if (solicitud == null || solicitud.monto() == null || solicitud.monto().signum() <= 0) {
            throw new IllegalArgumentException("El monto de la seña debe ser positivo");
        }
        BigDecimal monto = solicitud.monto().setScale(2, RoundingMode.HALF_UP);
        String clave = solicitud.idempotencyKey() == null || solicitud.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString() : solicitud.idempotencyKey();

        Optional<Senia> existente = senias.findByIdempotencyKey(clave);
        if (existente.isPresent()) {
            return respuesta(existente.get(), null);
        }

        String externalReference = "senia-" + UUID.randomUUID();
        Senia guardada = senias.saveAndFlush(new Senia(solicitud.turnoId(),
                solicitud.presupuestoId(), monto, externalReference, clave));
        String preferenceId = mercadoPago.crearPreferencia(
                guardada.getExternalReference(), guardada.getMonto(), clave);
        publicar("pago.solicitado", guardada, actor, "solicitud");
        return respuesta(guardada, preferenceId);
    }

    @Transactional
    public WebhookResult procesarWebhook(WebhookSenia evento, String requestId,
                                         ActorSimulado actor) {
        if (evento == null || evento.externalReference() == null
                || evento.externalReference().isBlank()) {
            throw new IllegalArgumentException("La referencia externa es obligatoria");
        }
        String clave = requestId != null && !requestId.isBlank()
                ? requestId : evento.externalReference() + "|" + evento.ts();
        if (!idempotencia.registrar(clave)) {
            Senia actual = senias.findByExternalReference(evento.externalReference()).orElse(null);
            return new WebhookResult(true, false, actual == null ? null : actual.getEstado(),
                    actual != null && actual.isConciliado());
        }

        Senia senia = senias.findByExternalReference(evento.externalReference())
                .orElseThrow(() -> new NoSuchElementException("Seña inexistente"));
        String estado = PaymentStatusMapper.estado(evento.status());
        if (!senia.aplicarEstado(estado, Instant.now())) {
            return new WebhookResult(false, false, senia.getEstado(), senia.isConciliado());
        }
        if (evento.paymentId() != null && !evento.paymentId().isBlank()) {
            senia.registrarPago(evento.paymentId());
        }
        senias.saveAndFlush(senia);

        String tipo = PaymentStatusMapper.tipoEvento(estado);
        if (tipo != null) {
            publicar(tipo, senia, actor, estado);
        }
        if (Senia.APROBADO.equals(estado)) {
            conciliar(senia.getId());
        }
        if (Senia.RECHAZADO.equals(estado)) {
            notificaciones.solicitar(senia.getId(), "pago.rechazado");
        }
        return new WebhookResult(false, true, senia.getEstado(), senia.isConciliado());
    }

    @Transactional
    public boolean conciliar(Long seniaId) {
        Senia senia = senias.findById(seniaId)
                .orElseThrow(() -> new NoSuchElementException("Seña inexistente"));
        if (!Senia.APROBADO.equals(senia.getEstado()) || senia.isConciliado()) {
            return senia.isConciliado();
        }
        boolean resuelve = (senia.getTurnoId() != null && turnos.existsById(senia.getTurnoId()))
                || (senia.getPresupuestoId() != null
                && presupuestos.existsById(senia.getPresupuestoId()));
        if (!resuelve) {
            return false;
        }
        senia.marcarConciliada();
        senias.saveAndFlush(senia);
        publicar("pago.conciliado", senia, ActorSimulado.system(), "conciliacion");
        return true;
    }

    @Transactional
    public RefundResponse reembolsar(Long seniaId, ActorSimulado actor) {
        Senia senia = senias.findById(seniaId)
                .orElseThrow(() -> new NoSuchElementException("Seña inexistente"));
        if (!Senia.APROBADO.equals(senia.getEstado())) {
            throw new IllegalStateException("Solo se puede reembolsar una seña aprobada");
        }
        String pago = senia.getPaymentId() == null ? senia.getExternalReference() : senia.getPaymentId();
        mercadoPago.reembolsar(pago, senia.getExternalReference(), senia.getMonto());
        senia.marcarReembolsada();
        senias.saveAndFlush(senia);
        if (senia.getTurnoId() != null) {
            turnos.findById(senia.getTurnoId()).ifPresent(
                    turno -> turno.cancelar(actor.nombre()));
        }
        publicar("pago.reembolsado", senia, actor, "reembolso total");
        return new RefundResponse(senia.getId(), senia.getEstado(), senia.getMonto());
    }

    private void publicar(String tipo, Senia senia, ActorSimulado actor, String motivo) {
        String correlationId = UUID.randomUUID().toString();
        auditoria.publish(new AuditEvent(tipo, "senia", senia.getId(), actor, Instant.now(),
                motivo, PagoAuditMetadata.de(senia, motivo, correlationId), correlationId));
    }

    private SeniaResponse respuesta(Senia senia, String preferenceId) {
        return new SeniaResponse(senia.getId(), senia.getExternalReference(), senia.getEstado(),
                senia.getMoneda(), senia.getMonto().toPlainString(), preferenceId);
    }
}
