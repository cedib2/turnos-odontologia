package com.turnosodontologia.domain.identidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
public class RolPermisoId implements Serializable {

    @Column(name = "rol_id", nullable = false)
    private Long rolId;

    @Column(name = "permiso_id", nullable = false)
    private Long permisoId;

    protected RolPermisoId() {
    }

    public RolPermisoId(Long rolId, Long permisoId) {
        this.rolId = rolId;
        this.permisoId = permisoId;
    }
}
