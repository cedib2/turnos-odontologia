package com.turnosodontologia.domain.identidad;

import com.turnosodontologia.domain.comun.AuditableEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "rol_permiso")
@Getter
public class RolPermiso extends AuditableEntity {

    @EmbeddedId
    private RolPermisoId id;

    @ManyToOne(optional = false)
    @MapsId("rolId")
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(optional = false)
    @MapsId("permisoId")
    @JoinColumn(name = "permiso_id", nullable = false)
    private Permiso permiso;

    protected RolPermiso() {
    }

    public RolPermiso(Rol rol, Permiso permiso) {
        this.rol = rol;
        this.permiso = permiso;
        this.id = new RolPermisoId(rol.getId(), permiso.getId());
    }
}
