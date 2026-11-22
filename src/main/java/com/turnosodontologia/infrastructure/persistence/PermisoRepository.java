package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.identidad.Ambito;
import com.turnosodontologia.domain.identidad.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByRecursoAndAccionAndAmbito(String recurso, String accion, Ambito ambito);
}
