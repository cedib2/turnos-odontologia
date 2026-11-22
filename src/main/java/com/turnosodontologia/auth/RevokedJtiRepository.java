package com.turnosodontologia.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedJtiRepository extends JpaRepository<RevokedJti, Long> {
}
