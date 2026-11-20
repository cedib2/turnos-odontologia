package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    @Query("SELECT b FROM Bloqueo b WHERE b.activo = true AND b.hasta > :desde AND b.desde < :hasta "
            + "AND (:profesionalId IS NULL OR b.profesionalId = :profesionalId) "
            + "AND (:sillonId IS NULL OR b.sillonId = :sillonId)")
    List<Bloqueo> findActivosEnRango(
            @Param("profesionalId") Long profesionalId,
            @Param("sillonId") Long sillonId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta);
}
