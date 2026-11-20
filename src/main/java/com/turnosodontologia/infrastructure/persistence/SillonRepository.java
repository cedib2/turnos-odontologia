package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Sillon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SillonRepository extends JpaRepository<Sillon, Long> {

    List<Sillon> findByActivoTrue();
}
