package com.turnosodontologia.domain.identidad;

import com.turnosodontologia.domain.comun.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(name = "permisos", uniqueConstraints = @UniqueConstraint(
        name = "ux_permisos_recurso_accion_ambito",
        columnNames = {"recurso", "accion", "ambito"}))
@Getter
public class Permiso extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permiso_seq")
    @SequenceGenerator(name = "permiso_seq", sequenceName = "permisos_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "recurso", nullable = false, length = 60)
    private String recurso;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambito", nullable = false, length = 16)
    private Ambito ambito;

    protected Permiso() {
    }

    public Permiso(String recurso, String accion, Ambito ambito) {
        this.recurso = recurso;
        this.accion = accion;
        this.ambito = ambito;
    }
}
