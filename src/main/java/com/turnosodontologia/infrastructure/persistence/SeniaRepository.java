package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.financiero.Senia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeniaRepository extends JpaRepository<Senia, Long> {

    Optional<Senia> findByExternalReference(String externalReference);

    Optional<Senia> findByIdempotencyKey(String idempotencyKey);
}
