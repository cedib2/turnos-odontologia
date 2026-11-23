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

import java.time.Instant;
import java.util.Objects;

/** Candidato de la lista de espera de C-19 (W-2, W-5). */
@Entity
@Table(name = "lista_espera")
@Getter
public class EntradaWaitlist extends AuditableEntity {

    public static final String ACTIVA = "activa";
    public static final String OFRECIDA = "ofrecida";
    public static final String ADJUDICADA = "adjudicada";
    public static final String EXPIRADA = "expirada";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lista_espera_seq")
    @SequenceGenerator(name = "lista_espera_seq", sequenceName = "lista_espera_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "candidato_id", nullable = false)
    private Long candidatoId;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "sillon_id", nullable = false)
    private Long sillonId;

    @Column(name = "inicio_deseado", nullable = false)
    private Instant inicioDeseado;

    @Column(name = "fin_deseado", nullable = false)
    private Instant finDeseado;

    @Column(name = "deposito_requerido", nullable = false)
    private boolean depositoRequerido;

    @Column(name = "senia_id")
    private Long seniaId;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = ACTIVA;

    @Column(name = "ofrecida_en")
    private Instant ofrecidaEn;

    @Column(name = "turno_id")
    private Long turnoId;

    protected EntradaWaitlist() {
    }

    public EntradaWaitlist(Long candidatoId, Long profesionalId, Long sillonId,
                           Instant inicioDeseado, Instant finDeseado, boolean depositoRequerido) {
        if (candidatoId == null || profesionalId == null || sillonId == null) {
            throw new IllegalArgumentException("Candidato, profesional y sillón son obligatorios");
        }
        if (inicioDeseado == null || finDeseado == null || !finDeseado.isAfter(inicioDeseado)) {
            throw new IllegalArgumentException("El rango deseado debe ser válido");
        }
        this.candidatoId = candidatoId;
        this.profesionalId = profesionalId;
        this.sillonId = sillonId;
        this.inicioDeseado = inicioDeseado;
        this.finDeseado = finDeseado;
        this.depositoRequerido = depositoRequerido;
    }

    public boolean cubre(Long profesional, Long sillon, Instant inicio, Instant fin) {
        return Objects.equals(profesionalId, profesional) && Objects.equals(sillonId, sillon)
                && !inicioDeseado.isAfter(inicio) && !finDeseado.isBefore(fin);
    }

    public void marcarOfrecida(Long turnoId, Instant ofrecidaEn, String autor) {
        if (!ACTIVA.equals(estado)) {
            throw new IllegalStateException("Solo una entrada activa puede ofrecerse");
        }
        this.estado = OFRECIDA;
        this.turnoId = turnoId;
        this.ofrecidaEn = ofrecidaEn;
        registrarActualizacion(autor);
    }

    public void marcarExpirada(String autor) {
        if (!OFRECIDA.equals(estado)) {
            throw new IllegalStateException("Solo una entrada ofrecida puede expirar");
        }
        this.estado = EXPIRADA;
        registrarActualizacion(autor);
    }

    public void marcarAdjudicada(String autor) {
        if (!OFRECIDA.equals(estado) && !ACTIVA.equals(estado)) {
            throw new IllegalStateException("Solo una entrada activa u ofrecida puede adjudicarse");
        }
        this.estado = ADJUDICADA;
        registrarActualizacion(autor);
    }

    public void vincularSenia(Long seniaId, String autor) {
        this.seniaId = seniaId;
        registrarActualizacion(autor);
    }
}
