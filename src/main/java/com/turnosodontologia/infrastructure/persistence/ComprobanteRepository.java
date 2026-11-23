package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.financiero.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findBySeniaIdAndTipo(Long seniaId, String tipo);
}
