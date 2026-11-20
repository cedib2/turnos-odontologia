package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Consultorio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {
}
