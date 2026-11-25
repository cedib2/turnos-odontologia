package com.turnosodontologia.domain.comun;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public void registrarCreacion(String autor) {
        this.createdBy = autor;
        this.updatedAt = this.createdAt;
        this.updatedBy = autor;
    }

    public void registrarActualizacion(String autor) {
        this.updatedAt = Instant.now();
        this.updatedBy = autor;
    }

    public void desactivar(String autor) {
        this.activo = false;
        registrarActualizacion(autor);
    }

    /** C-19 (W-4): reocupación de un hueco liberado por la waitlist. */
    public void activar(String autor) {
        this.activo = true;
        registrarActualizacion(autor);
    }
}
