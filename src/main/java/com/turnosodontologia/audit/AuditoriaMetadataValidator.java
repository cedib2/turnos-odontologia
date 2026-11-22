package com.turnosodontologia.audit;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AuditoriaMetadataValidator {

    public static final Set<String> CLAVES_PROHIBIDAS = Set.of(
            "password", "password_hash", "hash_password", "secret", "api_key",
            "access_token", "refresh_token", "token", "authorization", "jwt",
            "credentials", "dni", "documento_numero", "telefono", "email",
            "direccion", "fecha_nacimiento", "firma", "signature", "diagnostico",
            "historia_clinica", "ficha_clinica");

    private static final Set<String> NORMALIZADAS = CLAVES_PROHIBIDAS.stream()
            .map(AuditoriaMetadataValidator::normalizar)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    public Map<String, Object> validar(Map<String, Object> metadata) {
        validarValor(metadata);
        return metadata;
    }

    private void validarValor(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            mapa.forEach((clave, contenido) -> {
                if (normalizar(String.valueOf(clave)) != null
                        && NORMALIZADAS.contains(normalizar(String.valueOf(clave)))) {
                    throw new IllegalArgumentException("Clave de metadata prohibida: " + clave);
                }
                validarValor(contenido);
            });
        } else if (valor instanceof Iterable<?> iterable) {
            iterable.forEach(this::validarValor);
        }
    }

    private static String normalizar(String clave) {
        String sinAcentos = Normalizer.normalize(clave, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }
}
