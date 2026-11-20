package com.turnosodontologia.domain.agenda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "ocupaciones")
@Getter
public class Ocupacion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ocupacion_seq")
    @SequenceGenerator(name = "ocupacion_seq", sequenceName = "ocupaciones_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "turno_id")
    private Long turnoId;

    @Column(name = "bloqueo_id")
    private Long bloqueoId;

    @Column(name = "profesional_id")
    private Long profesionalId;

    @Column(name = "sillon_id")
    private Long sillonId;

    @Column(name = "inicio", nullable = false)
    private Instant inicio;

    @Column(name = "fin", nullable = false)
    private Instant fin;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Ocupacion() {
    }

    private Ocupacion(Long turnoId, Long bloqueoId, Long profesionalId, Long sillonId,
                      Instant inicio, Instant fin) {
        this.turnoId = turnoId;
        this.bloqueoId = bloqueoId;
        this.profesionalId = profesionalId;
        this.sillonId = sillonId;
        this.inicio = inicio;
        this.fin = fin;
    }

    public static Ocupacion paraTurno(Long turnoId, Long profesionalId, Long sillonId,
                                      Instant inicio, Instant fin) {
        return new Ocupacion(turnoId, null, profesionalId, sillonId, inicio, fin);
    }

    public static Ocupacion paraBloqueo(Long bloqueoId, Long profesionalId, Long sillonId,
                                        Instant inicio, Instant fin) {
        return new Ocupacion(null, bloqueoId, profesionalId, sillonId, inicio, fin);
    }

    public void desactivar() {
        this.activo = false;
    }
}
