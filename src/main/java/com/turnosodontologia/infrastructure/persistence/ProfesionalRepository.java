package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {

    List<Profesional> findByActivoTrue();

    Optional<Profesional> findByMatricula(String matricula);
}
