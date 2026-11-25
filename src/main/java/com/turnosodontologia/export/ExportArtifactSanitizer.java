package com.turnosodontologia.export;

import com.turnosodontologia.audit.AuditoriaMetadataValidator;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ExportArtifactSanitizer {

    private static final Set<String> PROHIBIDAS = java.util.stream.Stream.concat(
            AuditoriaMetadataValidator.CLAVES_PROHIBIDAS.stream(),
            java.util.stream.Stream.of("webhook_signature", "webhookSignature")).collect(
                    java.util.stream.Collectors.toUnmodifiableSet());

    public void validar(Map<String, Object> metadata) {
        validarValor(metadata);
    }

    private void validarValor(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            mapa.forEach((clave, contenido) -> {
                String normalizada = String.valueOf(clave).toLowerCase(Locale.ROOT)
                        .replace("_", "").replace("-", "");
                if (PROHIBIDAS.stream().anyMatch(prohibida -> normalizada.equals(
                        prohibida.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "")))) {
                    throw new IllegalArgumentException("Dato prohibido en artefacto de exportación: " + clave);
                }
                validarValor(contenido);
            });
        } else if (valor instanceof Iterable<?> iterable) {
            iterable.forEach(this::validarValor);
        }
    }
}
