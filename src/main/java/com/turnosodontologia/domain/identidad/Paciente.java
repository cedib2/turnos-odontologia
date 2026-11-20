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

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pacientes", uniqueConstraints = @UniqueConstraint(
        name = "ux_pacientes_documento",
        columnNames = {"documento_tipo", "documento_numero"}))
@Getter
public class Paciente extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paciente_seq")
    @SequenceGenerator(name = "paciente_seq", sequenceName = "pacientes_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "documento_tipo", nullable = false, length = 16)
    private DocumentoTipo documentoTipo;

    @Column(name = "documento_numero", nullable = false, length = 32)
    private String documentoNumero;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 120)
    private String apellido;

    @Column(name = "telefono", nullable = false, length = 40)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "consentimiento_datos", nullable = false)
    private boolean consentimientoDatos;

    @Column(name = "fecha_consentimiento")
    private Instant fechaConsentimiento;

    @Column(name = "version_politica", length = 40)
    private String versionPolitica;

    @Column(name = "finalidad", length = 255)
    private String finalidad;

    @Column(name = "canal", length = 40)
    private String canal;

    protected Paciente() {
    }

    public Paciente(DocumentoTipo documentoTipo, String documentoNumero,
                    String nombre, String apellido, String telefono) {
        this.documentoTipo = documentoTipo;
        this.documentoNumero = documentoNumero;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
    }

    public void registrarConsentimiento(boolean aceptado, Instant fecha, String version,
                                        String finalidad, String canal) {
        this.consentimientoDatos = aceptado;
        this.fechaConsentimiento = fecha;
        this.versionPolitica = version;
        this.finalidad = finalidad;
        this.canal = canal;
    }
}
