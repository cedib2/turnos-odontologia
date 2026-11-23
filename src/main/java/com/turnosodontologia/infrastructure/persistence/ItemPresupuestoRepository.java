package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.financiero.ItemPresupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPresupuestoRepository extends JpaRepository<ItemPresupuesto, Long> {
}
