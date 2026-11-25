package com.turnosodontologia.domain.waitlist;

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
import java.util.regex.Pattern;

/** Cifra publicable con metodología y período explícitos (W-9). */
@Entity
@Table(name = "metricas_ocupacion")
@Getter
public class MetricaOcupacion extends AuditableEntity {

    private static final Pattern PERIODO = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metrica_ocupacion_seq")
    @SequenceGenerator(name = "metrica_ocupacion_seq", sequenceName = "metricas_ocupacion_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "sillon_id", nullable = false)
    private Long sillonId;

    @Column(name = "periodo", nullable = false, length = 7)
    private String periodo;

    @Column(name = "ocupacion", precision = 7, scale = 4)
    private BigDecimal ocupacion;

    @Column(name = "ausentismo", precision = 7, scale = 4)
    private BigDecimal ausentismo;

    @Column(name = "metodologia", nullable = false, length = 2000)
    private String metodologia;

    @Column(name = "calculada_en", nullable = false)
    private Instant calculadaEn;

    protected MetricaOcupacion() {
    }

    public MetricaOcupacion(Long sillonId, String periodo, BigDecimal ocupacion,
                            BigDecimal ausentismo, String metodologia, Instant calculadaEn) {
        if (sillonId == null) {
            throw new IllegalArgumentException("El sillón es obligatorio");
        }
        if (periodo == null || !PERIODO.matcher(periodo).matches()) {
            throw new IllegalArgumentException("El período debe tener formato YYYY-MM");
        }
        if (metodologia == null || metodologia.isBlank()) {
            throw new IllegalArgumentException("La metodología es obligatoria");
        }
        if (calculadaEn == null) {
            throw new IllegalArgumentException("La fecha de cálculo es obligatoria");
        }
        this.sillonId = sillonId;
        this.periodo = periodo;
        this.ocupacion = ocupacion;
        this.ausentismo = ausentismo;
        this.metodologia = metodologia;
        this.calculadaEn = calculadaEn;
    }

    public void actualizar(BigDecimal ocupacion, BigDecimal ausentismo, String metodologia,
                           Instant calculadaEn, String autor) {
        if (metodologia == null || metodologia.isBlank()) {
            throw new IllegalArgumentException("La metodología es obligatoria");
        }
        this.ocupacion = ocupacion;
        this.ausentismo = ausentismo;
        this.metodologia = metodologia;
        this.calculadaEn = calculadaEn;
        registrarActualizacion(autor);
    }
}
