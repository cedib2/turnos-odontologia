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
@Table(name = "tratamientos")
@Getter
public class Tratamiento extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tratamiento_seq")
    @SequenceGenerator(name = "tratamiento_seq", sequenceName = "tratamientos_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    protected Tratamiento() {
    }

    public Tratamiento(String codigo, String nombre, int duracionMinutos) {
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración del tratamiento debe ser positiva");
        }
        this.codigo = codigo;
        this.nombre = nombre;
        this.duracionMinutos = duracionMinutos;
    }
}
