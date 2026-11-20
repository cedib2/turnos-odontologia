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

@Entity
@Table(name = "consultorios")
@Getter
public class Consultorio extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "consultorio_seq")
    @SequenceGenerator(name = "consultorio_seq", sequenceName = "consultorios_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    protected Consultorio() {
    }

    public Consultorio(String nombre) {
        this.nombre = nombre;
    }
}
