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

import java.time.LocalDate;

@Entity
@Table(name = "feriados")
@Getter
public class Feriado extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feriado_seq")
    @SequenceGenerator(name = "feriado_seq", sequenceName = "feriados_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "fecha", nullable = false, unique = true)
    private LocalDate fecha;

    @Column(name = "descripcion", nullable = false, length = 160)
    private String descripcion;

    @Column(name = "aplica_a_todos")
    private Boolean aplicaATodos;

    protected Feriado() {
    }

    public Feriado(LocalDate fecha, String descripcion) {
        this(fecha, descripcion, null);
    }

    public Feriado(LocalDate fecha, String descripcion, Boolean aplicaATodos) {
        validar(fecha, descripcion);
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.aplicaATodos = aplicaATodos;
    }

    public void actualizar(LocalDate fecha, String descripcion, Boolean aplicaATodos) {
        validar(fecha, descripcion);
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.aplicaATodos = aplicaATodos;
    }

    private static void validar(LocalDate fecha, String descripcion) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha del feriado es obligatoria");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del feriado es obligatoria");
        }
    }
}
