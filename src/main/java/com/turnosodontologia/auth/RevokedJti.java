package com.turnosodontologia.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_jti_revocados")
public class RevokedJti {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auth_jti_revocado_seq")
    @SequenceGenerator(name = "auth_jti_revocado_seq", sequenceName = "auth_jti_revocados_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "jti", nullable = false, length = 64, unique = true)
    private String jti;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private AuthSession sesion;

    @Column(name = "motivo", nullable = false, length = 40)
    private String motivo = "LOGOUT";

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    protected RevokedJti() {
    }

    public RevokedJti(String jti, AuthSession sesion, Instant revokedAt, String motivo) {
        this.jti = jti;
        this.sesion = sesion;
        this.revokedAt = revokedAt;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public String getJti() {
        return jti;
    }
}
