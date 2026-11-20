package com.turnosodontologia.domain.identidad;

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
@Table(name = "roles")
@Getter
public class Rol extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rol_seq")
    @SequenceGenerator(name = "rol_seq", sequenceName = "roles_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    protected Rol() {
    }

    public Rol(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }
}
