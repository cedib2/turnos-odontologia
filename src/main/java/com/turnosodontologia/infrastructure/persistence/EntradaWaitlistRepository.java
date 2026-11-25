package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.waitlist.EntradaWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EntradaWaitlistRepository extends JpaRepository<EntradaWaitlist, Long> {

    Optional<EntradaWaitlist> findByCandidatoIdAndInicioDeseadoAndFinDeseadoAndActivoTrue(
            Long candidatoId, Instant inicioDeseado, Instant finDeseado);

    List<EntradaWaitlist> findByCandidatoIdAndActivoTrueOrderByCreatedAtAsc(Long candidatoId);

    List<EntradaWaitlist> findByEstadoAndActivoTrueOrderByCreatedAtAsc(String estado);

    @Query("SELECT e FROM EntradaWaitlist e WHERE e.activo = true AND e.estado = :estado "
            + "AND e.turnoId = :turnoId")
    Optional<EntradaWaitlist> findByEstadoAndTurnoId(@Param("estado") String estado,
                                                     @Param("turnoId") Long turnoId);
}
