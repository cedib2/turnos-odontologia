package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    @Query("SELECT t FROM Turno t WHERE t.activo = true AND t.fin > :desde AND t.inicio < :hasta "
            + "AND (:profesionalId IS NULL OR t.profesional.id = :profesionalId) "
            + "AND (:sillonId IS NULL OR t.sillon.id = :sillonId)")
    List<Turno> findActivosEnRango(
            @Param("profesionalId") Long profesionalId,
            @Param("sillonId") Long sillonId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta);

    java.util.Optional<Turno> findByTokenPublico(String tokenPublico);
}
