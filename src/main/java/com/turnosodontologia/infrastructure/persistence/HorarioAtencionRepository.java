package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {

    List<HorarioAtencion> findByDiaSemanaAndProfesionalIdIsNull(int diaSemana);

    List<HorarioAtencion> findByDiaSemanaAndProfesionalId(int diaSemana, Long profesionalId);
}
