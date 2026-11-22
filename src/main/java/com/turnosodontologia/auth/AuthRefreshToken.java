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
@Table(name = "auth_tokens_refresh")
public class AuthRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auth_refresh_seq")
    @SequenceGenerator(name = "auth_refresh_seq", sequenceName = "auth_tokens_refresh_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private AuthSession sesion;

    @Column(name = "hash_token", nullable = false, length = 128, unique = true)
    private String hashToken;

    @Column(name = "jti", nullable = false, length = 64)
    private String jti;

    @Column(name = "familia_id", nullable = false, length = 64)
    private String familiaId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotated_from_id")
    private AuthRefreshToken rotatedFrom;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthRefreshToken() {
    }

    public AuthRefreshToken(AuthSession sesion, String hashToken, String jti, String familiaId,
                            Instant issuedAt, Instant expiresAt) {
        this.sesion = sesion;
        this.hashToken = hashToken;
        this.jti = jti;
        this.familiaId = familiaId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.createdAt = issuedAt;
    }

    public Long getId() {
        return id;
    }

    public AuthSession getSesion() {
        return sesion;
    }

    public String getHashToken() {
        return hashToken;
    }

    public String getFamiliaId() {
        return familiaId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void marcarUsado(Instant fecha) {
        this.usedAt = fecha;
    }

    public void revocar(Instant fecha) {
        this.revokedAt = fecha;
    }

    public void rotarDesde(AuthRefreshToken anterior) {
        this.rotatedFrom = anterior;
    }
}
