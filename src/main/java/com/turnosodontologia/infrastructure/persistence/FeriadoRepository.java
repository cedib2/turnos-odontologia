package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

    Optional<Feriado> findByFecha(LocalDate fecha);
}
