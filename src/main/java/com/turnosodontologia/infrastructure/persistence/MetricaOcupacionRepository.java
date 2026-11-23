package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.waitlist.MetricaOcupacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetricaOcupacionRepository extends JpaRepository<MetricaOcupacion, Long> {

    Optional<MetricaOcupacion> findBySillonIdAndPeriodoAndActivoTrue(Long sillonId, String periodo);
}
