package com.turnosodontologia.domain.agenda;

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

@Entity
@Table(name = "bloqueos")
@Getter
public class Bloqueo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bloqueo_seq")
    @SequenceGenerator(name = "bloqueo_seq", sequenceName = "bloqueos_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "profesional_id")
    private Long profesionalId;

    @Column(name = "sillon_id")
    private Long sillonId;

    @Column(name = "desde", nullable = false)
    private Instant desde;

    @Column(name = "hasta", nullable = false)
    private Instant hasta;

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    protected Bloqueo() {
    }

    public Bloqueo(Long profesionalId, Long sillonId, Instant desde, Instant hasta, String motivo) {
        this.profesionalId = profesionalId;
        this.sillonId = sillonId;
        this.desde = desde;
        this.hasta = hasta;
        this.motivo = motivo;
    }
}
