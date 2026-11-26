package com.turnosodontologia.auth;

import com.turnosodontologia.domain.identidad.ProveedorAutenticacion;
import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    static final long ACCESS_SECONDS = 15 * 60;

    private final UsuarioRepository usuarios;
    private final AuthSessionRepository sesiones;
    private final AuthRefreshTokenRepository refreshes;
    private final AuthRevocationService revocaciones;
    private final PasswordHasher hasher;
    private final JwtService jwt;
    private final SecureRandom random = new SecureRandom();
    private final long refreshSeconds;

    public AuthService(UsuarioRepository usuarios,
                       AuthSessionRepository sesiones,
                       AuthRefreshTokenRepository refreshes,
                       AuthRevocationService revocaciones,
                       PasswordHasher hasher,
                       JwtService jwt,
                       @Value("${auth.refresh-days:7}") long refreshDays) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.refreshes = refreshes;
        this.revocaciones = revocaciones;
        this.hasher = hasher;
        this.jwt = jwt;
        this.refreshSeconds = refreshDays * 24 * 60 * 60;
    }

    public LoginResult login(String email, String rawCredential, String ip, String userAgent) {
        Usuario usuario = usuarios.findByEmail(email).orElse(null);
        boolean local = usuario != null
                && usuario.isActivo()
                && usuario.getProveedor() == ProveedorAutenticacion.LOCAL
                && usuario.getHashPassword() != null;
        String conocido = local ? usuario.getHashPassword() : hasher.dummyVerificationHash();
        boolean coincide = hasher.matches(rawCredential, conocido);

        if (usuario == null || !usuario.isActivo() || !local || !coincide) {
            throw new InvalidCredentialsException();
        }

        Instant ahora = Instant.now();
        String jti = UUID.randomUUID().toString();
        String refresh = nuevoValorOpaco();
        AuthSession sesion = sesiones.saveAndFlush(new AuthSession(
                usuario, jti, ahora, ahora.plusSeconds(ACCESS_SECONDS), ip, userAgent));
        refreshes.saveAndFlush(new AuthRefreshToken(
                sesion, hash(refresh), jti, UUID.randomUUID().toString(),
                ahora, ahora.plusSeconds(refreshSeconds)));
        usuario.registrarAcceso(ahora);

        String access = jwt.emitir(usuario.getId(), usuario.getRol().getCodigo(), jti, ahora);
        return new LoginResult(access, refresh, jti, usuario.getRol().getCodigo(), usuario.getId());
    }

    public RotationResult rotar(String refresh) {
        if (refresh == null || refresh.isBlank()) {
            return RotationResult.rechazado();
        }
        AuthRefreshToken anterior = refreshes.findByHashTokenForUpdate(hash(refresh)).orElse(null);
        if (anterior == null) {
            return RotationResult.rechazado();
        }
        Instant ahora = Instant.now();

        if (anterior.getUsedAt() != null || anterior.getRevokedAt() != null) {
            revocarFamilia(anterior, ahora);
            return RotationResult.rechazado();
        }
        if (!anterior.getExpiresAt().isAfter(ahora)) {
            return RotationResult.rechazado();
        }
        AuthSession sesion = anterior.getSesion();
        if (sesion.getEstado() != SessionState.ACTIVA || !sesion.getExpiresAt().isAfter(ahora)) {
            return RotationResult.rechazado();
        }

        anterior.marcarUsado(ahora);
        sesion.registrarUso(ahora);
        String nuevoRefresh = nuevoValorOpaco();
        AuthRefreshToken siguiente = new AuthRefreshToken(
                sesion, hash(nuevoRefresh), UUID.randomUUID().toString(), anterior.getFamiliaId(),
                ahora, anterior.getExpiresAt());
        siguiente.rotarDesde(anterior);
        refreshes.saveAndFlush(siguiente);

        String access = jwt.emitir(sesion.getUsuario().getId(), sesion.getUsuario().getRol().getCodigo(),
                sesion.getJti(), ahora);
        return RotationResult.aceptado(new LoginResult(
                access, nuevoRefresh, sesion.getJti(), sesion.getUsuario().getRol().getCodigo(),
                sesion.getUsuario().getId()));
    }

    public void logout(String jti) {
        revocaciones.revocarSesion(jti, Instant.now());
    }

    private void revocarFamilia(AuthRefreshToken anterior, Instant ahora) {
        revocaciones.revocarSesion(anterior.getSesion().getJti(), ahora);
    }

    private String nuevoValorOpaco() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record LoginResult(String access, String refresh, String jti, String rol, Long usuarioId) {
    }

    public record RotationResult(LoginResult credenciales) {

        public static RotationResult aceptado(LoginResult credenciales) {
            return new RotationResult(credenciales);
        }

        public static RotationResult rechazado() {
            return new RotationResult(null);
        }

        public boolean esValido() {
            return credenciales != null;
        }
    }
}
