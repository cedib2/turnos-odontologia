package com.turnosodontologia.auth;

import com.turnosodontologia.domain.identidad.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "auth_sesiones")
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auth_sesiones_seq")
    @SequenceGenerator(name = "auth_sesiones_seq", sequenceName = "auth_sesiones_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "jti", nullable = false, length = 64, unique = true)
    private String jti;

    @Column(name = "estado", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SessionState estado = SessionState.ACTIVA;

    @Column(name = "ip_origen", length = 64)
    private String ipOrigen;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AuthSession() {
    }

    public AuthSession(Usuario usuario, String jti, Instant createdAt, Instant expiresAt,
                       String ipOrigen, String userAgent) {
        this.usuario = usuario;
        this.jti = jti;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.ipOrigen = ipOrigen;
        this.userAgent = userAgent;
        this.lastUsedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getJti() {
        return jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public SessionState getEstado() {
        return estado;
    }

    public void registrarUso(Instant fecha) {
        this.lastUsedAt = fecha;
    }

    public void revocar(Instant fecha) {
        this.estado = SessionState.REVOCADA;
        this.revokedAt = fecha;
    }
}
