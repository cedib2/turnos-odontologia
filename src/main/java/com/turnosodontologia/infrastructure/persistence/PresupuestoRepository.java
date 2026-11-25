package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.financiero.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findAllByOrderByIdAsc();
}
