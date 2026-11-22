package com.turnosodontologia.publictoken;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PublicTokenService {

    private final PublicTokenSigner firmador;
    private final PublicTokenMetadataStore metadatos;
    private final AuditEventPublisher eventos;
    private final Clock reloj;
    private final PublicTokenSanitizer sanitizador;

    public PublicTokenService(PublicTokenSigner firmador, PublicTokenMetadataStore metadatos,
                              AuditEventPublisher eventos, Clock reloj) {
        this(firmador, metadatos, eventos, reloj, new PublicTokenSanitizer());
    }

    public PublicTokenService(PublicTokenSigner firmador, PublicTokenMetadataStore metadatos,
                              AuditEventPublisher eventos, Clock reloj,
                              PublicTokenSanitizer sanitizador) {
        this.firmador = firmador;
        this.metadatos = metadatos;
        this.eventos = eventos;
        this.reloj = reloj;
        this.sanitizador = sanitizador;
    }

    public String emitir(PublicTokenClaims claims) {
        validarContrato(claims);
        PublicTokenClaims normalizada = new PublicTokenClaims(claims.jti(), claims.iss(), claims.sub(),
                claims.pacienteId(), claims.scope(), claims.iat(),
                claims.iat() + claims.scope().vigenciaSegundos(), claims.turnoId(), false);
        String valor = firmador.emitir(normalizada);
        metadatos.registrar(normalizada, hash(valor));
        return valor;
    }

    public Validacion validar(String valor) {
        if (!firmador.firmaValida(valor)) {
            return new Validacion(false, "token_no_valido", null, false);
        }
        PublicTokenClaims claims = firmador.verificar(valor);
        if (!firmador.emisor().equals(claims.iss())) {
            return new Validacion(false, "token_no_valido", null, false);
        }
        if (claims.exp() <= reloj.instant().getEpochSecond()) {
            return new Validacion(false, "token_vencido", null, true);
        }
        if (!metadatos.estaDisponible(claims.jti())) {
            return new Validacion(false, "jti_reutilizado", null, false);
        }
        if (claims.scope() == PublicTokenScope.HISTORIA && claims.identidadVerificada()) {
            return new Validacion(false, "token_no_valido", null, false);
        }
        if (claims.scope() == PublicTokenScope.HISTORIA) {
            publicarDenegacionHistoria(claims);
            return new Validacion(false, "identidad_no_verificada", null, false);
        }
        return new Validacion(true, "ok", claims, false);
    }

    public Rotacion rotar(String valor) {
        if (!firmador.firmaValida(valor)) {
            return new Rotacion(false, "token_no_valido", null);
        }
        PublicTokenClaims anterior = firmador.verificar(valor);
        if (!firmador.emisor().equals(anterior.iss())) {
            return new Rotacion(false, "token_no_valido", null);
        }
        if (anterior.exp() <= reloj.instant().getEpochSecond()) {
            return new Rotacion(false, "token_vencido", null);
        }
        if (anterior.scope() != PublicTokenScope.GESTIONAR) {
            return new Rotacion(false, "rotacion_no_permitida", null);
        }
        if (!metadatos.estaDisponible(anterior.jti())) {
            return new Rotacion(false, "jti_reutilizado", null);
        }
        long emitido = reloj.instant().getEpochSecond();
        PublicTokenClaims nuevo = new PublicTokenClaims("jti-" + UUID.randomUUID(), anterior.iss(),
                anterior.sub(), anterior.pacienteId(), PublicTokenScope.GESTIONAR, emitido,
                emitido + PublicTokenScope.GESTIONAR.vigenciaSegundos(), anterior.turnoId(), false);
        String nuevoValor = firmador.emitir(nuevo);
        metadatos.rotar(anterior.jti(), nuevo, hash(nuevoValor));
        return new Rotacion(true, "ok", nuevoValor);
    }

    private void validarContrato(PublicTokenClaims claims) {
        if (claims.identidadVerificada()) {
            throw new IllegalArgumentException("identidad_no_verificada");
        }
        switch (claims.scope()) {
            case RESERVAR -> {
                if (claims.turnoId() != null) {
                    throw new IllegalArgumentException("vinculacion_invalida");
                }
            }
            case GESTIONAR -> {
                if (claims.turnoId() == null) {
                    throw new IllegalArgumentException("vinculacion_invalida");
                }
            }
            case HISTORIA -> {
                if (claims.pacienteId() == null) {
                    throw new IllegalArgumentException("vinculacion_invalida");
                }
            }
        }
    }

    private void publicarDenegacionHistoria(PublicTokenClaims claims) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jti", claims.jti());
        metadata.put("scope", claims.scope().name());
        metadata.put("pacienteId", claims.pacienteId());
        eventos.publish(AuditEvent.de("publictoken.historia.denegado", "publictoken", null,
                new ActorSimulado("paciente", "SIMULADO"), "identidad_no_verificada",
                sanitizador.metadata(metadata), UUID.randomUUID().toString()));
    }

    private String hash(String valor) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder texto = new StringBuilder();
            for (byte parte : resumen) {
                texto.append(String.format("%02x", parte));
            }
            return texto.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Clock reloj() {
        return reloj;
    }

    public record Validacion(boolean permitido, String motivo, PublicTokenClaims claims,
                             boolean requiereNuevoEnlace) {
    }

    public record Rotacion(boolean permitido, String motivo, String token) {
    }
}
