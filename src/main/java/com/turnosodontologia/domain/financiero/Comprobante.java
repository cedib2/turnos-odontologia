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
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Comprobante ARCA emitido a partir de una seña cobrada (C-16). Estado como texto
 * según AR-9; las transiciones están acotadas en el propio agregado.
 */
@Entity
@Table(name = "comprobantes")
@Getter
public class Comprobante extends AuditableEntity {

    public static final String TIPO_FACTURA = "factura";
    public static final String TIPO_NOTA_CREDITO = "nota_credito";
    public static final String PENDIENTE = "pendiente";
    public static final String EMITIDO = "emitido";
    public static final String RECHAZADO = "rechazado";
    public static final String ANULADO = "anulado";
    public static final String MONEDA = "ARS";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comprobante_seq")
    @SequenceGenerator(name = "comprobante_seq", sequenceName = "comprobantes_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "senia_id", nullable = false)
    private Long seniaId;

    @Column(name = "presupuesto_id")
    private Long presupuestoId;

    @Column(name = "turno_id")
    private Long turnoId;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = TIPO_FACTURA;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = PENDIENTE;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = MONEDA;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "concepto", nullable = false, length = 255)
    private String concepto;

    @Column(name = "cae", length = 64)
    private String cae;

    @Column(name = "fecha_emision")
    private Instant fechaEmision;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = 2000)
    private String ultimoError;

    @Column(name = "conciliado", nullable = false)
    private boolean conciliado;

    protected Comprobante() {
    }

    public Comprobante(Long seniaId, Long presupuestoId, Long turnoId, BigDecimal monto,
                       String concepto) {
        this(seniaId, presupuestoId, turnoId, TIPO_FACTURA, monto, concepto);
    }

    public Comprobante(Long seniaId, Long presupuestoId, Long turnoId, String tipo,
                       BigDecimal monto, String concepto) {
        if (seniaId == null) {
            throw new IllegalArgumentException("La seña del comprobante es obligatoria");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto del comprobante debe ser positivo");
        }
        if (concepto == null || concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto del comprobante es obligatorio");
        }
        this.seniaId = seniaId;
        this.presupuestoId = presupuestoId;
        this.turnoId = turnoId;
        this.tipo = tipo;
        this.monto = monto.setScale(2, RoundingMode.HALF_UP);
        this.concepto = concepto;
        registrarCreacion("sistema");
    }

    /** Confirma la emisión. Solo es válido desde {@code pendiente}. */
    public boolean emitir(String cae, Instant fecha) {
        if (!PENDIENTE.equals(estado) || cae == null || cae.isBlank()) {
            return false;
        }
        this.estado = EMITIDO;
        this.cae = cae;
        this.fechaEmision = fecha == null ? Instant.now() : fecha;
        this.ultimoError = null;
        registrarActualizacion("facturacion");
        return true;
    }

    /** Registra un rechazo del proveedor. Solo es válido desde {@code pendiente}. */
    public boolean rechazar(String errorSaneado) {
        if (!PENDIENTE.equals(estado)) {
            return false;
        }
        this.estado = RECHAZADO;
        this.ultimoError = errorSaneado;
        registrarActualizacion("facturacion");
        return true;
    }

    /** Prepara un reintento manual desde {@code rechazado} incrementando el intento. */
    public boolean prepararReintento() {
        if (!RECHAZADO.equals(estado)) {
            return false;
        }
        this.estado = PENDIENTE;
        this.intentos = intentos + 1;
        registrarActualizacion("reintento");
        return true;
    }

    /** Contabiliza el intento inicial de emisión. */
    public void contarIntento() {
        this.intentos = intentos + 1;
    }

    /** Anula la factura por nota de crédito. Solo es válido desde {@code emitido}. */
    public boolean anular() {
        if (!EMITIDO.equals(estado)) {
            return false;
        }
        this.estado = ANULADO;
        registrarActualizacion("nota_credito");
        return true;
    }

    /** Marca la conciliación solo si hay emisión confirmada con CAE. */
    public boolean marcarConciliada() {
        if (!EMITIDO.equals(estado) || cae == null || cae.isBlank() || conciliado) {
            return false;
        }
        this.conciliado = true;
        registrarActualizacion("conciliacion");
        return true;
    }

    public boolean esFacturaEmitida() {
        return TIPO_FACTURA.equals(tipo) && EMITIDO.equals(estado);
    }

    public boolean esElegibleParaConciliacion() {
        return EMITIDO.equals(estado) && cae != null && !cae.isBlank() && !conciliado;
    }
}
