package com.turnosodontologia.domain.agenda;

import com.turnosodontologia.domain.comun.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "sillones")
@Getter
public class Sillon extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sillon_seq")
    @SequenceGenerator(name = "sillon_seq", sequenceName = "sillones_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "numero", nullable = false, length = 40)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "consultorio_id", nullable = false)
    private Consultorio consultorio;

    protected Sillon() {
    }

    public Sillon(String numero, Consultorio consultorio) {
        this.numero = numero;
        this.consultorio = consultorio;
    }
}
