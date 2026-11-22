package com.turnosodontologia.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRevokedJtiRepository extends JpaRepository<AuthRevokedJti, Long> {

    boolean existsByJti(String jti);
}
