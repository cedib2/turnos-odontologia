package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.identidad.Rol;
import com.turnosodontologia.domain.identidad.RolPermiso;
import com.turnosodontologia.domain.identidad.RolPermisoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, RolPermisoId> {

    List<RolPermiso> findByRol(Rol rol);
}
