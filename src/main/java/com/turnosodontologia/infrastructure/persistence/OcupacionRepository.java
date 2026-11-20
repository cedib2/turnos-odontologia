package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Ocupacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OcupacionRepository extends JpaRepository<Ocupacion, Long> {

    long countByActivoTrue();

    java.util.Optional<Ocupacion> findByTurnoIdAndActivoTrue(Long turnoId);

    @Query("SELECT o FROM Ocupacion o WHERE o.activo = true AND o.fin > :desde AND o.inicio < :hasta "
            + "AND ((:profesionalId IS NOT NULL AND o.profesionalId = :profesionalId) "
            + "OR (:sillonId IS NOT NULL AND o.sillonId = :sillonId))")
    List<Ocupacion> findActivosQueSolapan(
            @Param("profesionalId") Long profesionalId,
            @Param("sillonId") Long sillonId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta);
}
