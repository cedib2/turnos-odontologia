package com.turnosodontologia.domain.financiero;

import com.turnosodontologia.domain.comun.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Seña cobrada con Mercado Pago (C-15). Los estados son texto según MP-4 y las
 * transiciones solo se permiten desde {@code pendiente}.
 */
@Entity
@Table(name = "senias")
@Getter
public class Senia extends AuditableEntity {

    public static final String MONEDA = "ARS";
    public static final String PROVEEDOR = "mercadopago";
    public static final String PENDIENTE = "pendiente";
    public static final String APROBADO = "aprobado";
    public static final String RECHAZADO = "rechazado";
    public static final String CANCELADO = "cancelado";
    public static final String REEMBOLSADO = "reembolsado";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "senia_seq")
    @SequenceGenerator(name = "senia_seq", sequenceName = "senias_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "turno_id")
    private Long turnoId;

    @Column(name = "presupuesto_id")
    private Long presupuestoId;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = MONEDA;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = PENDIENTE;

    @Column(name = "proveedor", nullable = false, length = 30)
    private String proveedor = PROVEEDOR;

    @Column(name = "payment_id", length = 64)
    private String paymentId;

    @Column(name = "external_reference", nullable = false, length = 128)
    private String externalReference;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "fecha_aprobacion")
    private Instant fechaAprobacion;

    @Column(name = "conciliado", nullable = false)
    private boolean conciliado;

    protected Senia() {
    }

    public Senia(Long turnoId, Long presupuestoId, BigDecimal monto,
                 String externalReference, String idempotencyKey) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto de la seña debe ser positivo");
        }
        if (externalReference == null || externalReference.isBlank()) {
            throw new IllegalArgumentException("La referencia externa es obligatoria");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("La clave de idempotencia es obligatoria");
        }
        this.turnoId = turnoId;
        this.presupuestoId = presupuestoId;
        this.monto = monto.setScale(2, java.math.RoundingMode.HALF_UP);
        this.externalReference = externalReference;
        this.idempotencyKey = idempotencyKey;
        registrarCreacion("sistema");
    }

    /**
     * Aplica un estado observado en el webhook. Devuelve {@code false} cuando la
     * seña ya es terminal, para descartar eventos tardíos sin efectos (MP-4).
     */
    public boolean aplicarEstado(String nuevoEstado, Instant observado) {
        if (!PENDIENTE.equals(this.estado)) {
            return false;
        }
        this.estado = nuevoEstado;
        if (APROBADO.equals(nuevoEstado)) {
            this.fechaAprobacion = observado;
        }
        registrarActualizacion("webhook");
        return true;
    }

    public void registrarPago(String paymentId) {
        this.paymentId = paymentId;
        registrarActualizacion("webhook");
    }

    public void marcarConciliada() {
        this.conciliado = true;
        registrarActualizacion("conciliacion");
    }

    public void marcarReembolsada() {
        this.estado = REEMBOLSADO;
        registrarActualizacion("reembolso");
    }
}
