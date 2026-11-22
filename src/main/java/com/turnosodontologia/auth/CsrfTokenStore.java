package com.turnosodontologia.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CsrfTokenStore {

    private final Set<String> vigentes = ConcurrentHashMap.newKeySet();
    private final SecureRandom random = new SecureRandom();

    public String emitir() {
        String valor = nuevoValor();
        vigentes.add(valor);
        return valor;
    }

    public boolean esValido(String cabecera, String cookie) {
        if (cabecera == null || cookie == null || cabecera.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(cabecera.getBytes(StandardCharsets.UTF_8),
                cookie.getBytes(StandardCharsets.UTF_8))
                && vigentes.contains(cabecera);
    }

    public String rotar(String anterior) {
        vigentes.remove(anterior);
        return emitir();
    }

    private String nuevoValor() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
