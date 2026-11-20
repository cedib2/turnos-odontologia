package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.identidad.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
}
