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

import java.time.LocalTime;

@Entity
@Table(name = "horarios")
@Getter
public class HorarioAtencion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "horario_seq")
    @SequenceGenerator(name = "horario_seq", sequenceName = "horarios_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "profesional_id")
    private Long profesionalId;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "desde", nullable = false)
    private LocalTime desde;

    @Column(name = "hasta", nullable = false)
    private LocalTime hasta;

    protected HorarioAtencion() {
    }

    public HorarioAtencion(Integer profesionalId, int diaSemana, LocalTime desde, LocalTime hasta) {
        validar(diaSemana, desde, hasta);
        this.profesionalId = profesionalId == null ? null : profesionalId.longValue();
        this.diaSemana = diaSemana;
        this.desde = desde;
        this.hasta = hasta;
    }

    public void actualizar(Integer profesionalId, int diaSemana, LocalTime desde, LocalTime hasta) {
        validar(diaSemana, desde, hasta);
        this.profesionalId = profesionalId == null ? null : profesionalId.longValue();
        this.diaSemana = diaSemana;
        this.desde = desde;
        this.hasta = hasta;
    }

    private static void validar(int diaSemana, LocalTime desde, LocalTime hasta) {
        if (diaSemana < 1 || diaSemana > 7) {
            throw new IllegalArgumentException("El día de la semana debe estar entre 1 y 7");
        }
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Los horarios desde y hasta son obligatorios");
        }
        if (!hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El horario debe terminar después de empezar");
        }
    }
}
