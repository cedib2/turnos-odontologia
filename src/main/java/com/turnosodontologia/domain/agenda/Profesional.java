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
@Table(name = "profesionales")
@Getter
public class Profesional extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profesional_seq")
    @SequenceGenerator(name = "profesional_seq", sequenceName = "profesionales_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "matricula", nullable = false, length = 40, unique = true)
    private String matricula;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 120)
    private String apellido;

    @Column(name = "especialidad", length = 120)
    private String especialidad;

    protected Profesional() {
    }

    public Profesional(String matricula, String nombre, String apellido) {
        this.matricula = matricula;
        this.nombre = nombre;
        this.apellido = apellido;
    }
}
