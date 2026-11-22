package com.turnosodontologia.domain.agenda;

import com.turnosodontologia.domain.comun.AuditableEntity;
import com.turnosodontologia.domain.identidad.Paciente;
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

import java.time.Instant;

@Entity
@Table(name = "turnos")
@Getter
public class Turno extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "turno_seq")
    @SequenceGenerator(name = "turno_seq", sequenceName = "turnos_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sillon_id", nullable = false)
    private Sillon sillon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_tratamiento_id", nullable = false)
    private Tratamiento tratamiento;

    @Column(name = "inicio", nullable = false)
    private Instant inicio;

    @Column(name = "fin", nullable = false)
    private Instant fin;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    @Column(name = "origen", nullable = false, length = 40)
    private String origen;

    @Column(name = "token_publico", length = 120)
    private String tokenPublico;

    @Column(name = "es_sobreturno", nullable = false)
    private boolean esSobreturno;

    @Column(name = "motivo_override", length = 255)
    private String motivoOverride;

    protected Turno() {
    }

    public Turno(Paciente paciente, Profesional profesional, Sillon sillon, Tratamiento tratamiento,
                 Instant inicio, Instant fin, int duracionMinutos, String estado, String origen,
                 String tokenPublico) {
        this.paciente = paciente;
        this.profesional = profesional;
        this.sillon = sillon;
        this.tratamiento = tratamiento;
        this.inicio = inicio;
        this.fin = fin;
        this.duracionMinutos = duracionMinutos;
        this.estado = estado;
        this.origen = origen;
        this.tokenPublico = tokenPublico;
    }

    public void asignarTokenPublico(String jti, String autor) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("El jti del token público es obligatorio");
        }
        this.tokenPublico = jti;
        registrarActualizacion(autor);
    }

    public void confirmar(String autor) {
        if ("cancelado".equals(this.estado)) {
            throw new IllegalArgumentException("Un turno cancelado no puede confirmarse");
        }
        this.estado = "confirmado";
        registrarActualizacion(autor);
    }

    public void cancelar(String autor) {
        this.estado = "cancelado";
        desactivar(autor);
    }

    public void reprogramar(Instant nuevoInicio, Instant nuevoFin, String autor) {
        if (nuevoInicio == null || nuevoFin == null || !nuevoFin.isAfter(nuevoInicio)) {
            throw new IllegalArgumentException("El rango reprogramado debe ser válido");
        }
        this.inicio = nuevoInicio;
        this.fin = nuevoFin;
        registrarActualizacion(autor);
    }

    public void marcarSobreturno(String motivo, String autor) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo del sobreturno no puede estar vacío");
        }
        this.esSobreturno = true;
        this.motivoOverride = motivo;
        registrarActualizacion(autor);
    }
}
