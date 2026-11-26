package com.turnosodontologia.domain.identidad;

import com.turnosodontologia.domain.comun.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "usuarios")
@Getter
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_seq")
    @SequenceGenerator(name = "usuario_seq", sequenceName = "usuarios_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "email", nullable = false, length = 160, unique = true)
    private String email;

    @Column(name = "hash_password", length = 255)
    private String hashPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor", nullable = false, length = 20)
    private ProveedorAutenticacion proveedor;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    /** Profesional del staff que representa este usuario (V033); define el ámbito {@code propio}. */
    @Column(name = "profesional_id")
    private Long profesionalId;

    protected Usuario() {
    }

    public Usuario(Rol rol, String email, String hashPassword, ProveedorAutenticacion proveedor) {
        this.rol = rol;
        this.email = email;
        this.hashPassword = hashPassword;
        this.proveedor = proveedor;
    }

    public void registrarAcceso(Instant fecha) {
        this.ultimoAcceso = fecha;
    }

    /** Vincula (o desvincula con {@code null}) el usuario con un profesional. */
    public void vincularProfesional(Long profesionalId) {
        this.profesionalId = profesionalId;
    }
}
