package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Tratamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

    Optional<Tratamiento> findByCodigo(String codigo);
}
