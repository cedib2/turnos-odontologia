package com.turnosodontologia.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByHashToken(String hashToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AuthRefreshToken token where token.hashToken = :hashToken")
    Optional<AuthRefreshToken> findByHashTokenForUpdate(@Param("hashToken") String hashToken);

    List<AuthRefreshToken> findByFamiliaId(String familiaId);

    List<AuthRefreshToken> findBySesionId(Long sesionId);
}
