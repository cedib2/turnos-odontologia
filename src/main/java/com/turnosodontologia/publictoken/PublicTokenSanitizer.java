package com.turnosodontologia.publictoken;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PublicTokenSanitizer {

    private static final String ENMASCARADO = "***";
    private static final Pattern COMPLETO = Pattern.compile(
            "[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}");
    private static final Pattern FRAGMENTO = Pattern.compile(
            "(?<![A-Za-z0-9_-])[A-Za-z0-9_-]{20,}(?![A-Za-z0-9_-])");

    public String uri(String valor) {
        return ocultar(valor);
    }

    public String mensaje(String valor) {
        return ocultar(valor);
    }

    public Map<String, Object> metadata(Map<String, Object> datos) {
        if (datos == null) {
            return Map.of();
        }
        Map<String, Object> salida = new LinkedHashMap<>();
        datos.forEach((clave, valor) -> salida.put(clave, sanear(valor)));
        return Collections.unmodifiableMap(salida);
    }

    private Object sanear(Object valor) {
        if (valor instanceof String texto) {
            return ocultar(texto);
        }
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> salida = new LinkedHashMap<>();
            mapa.forEach((clave, contenido) -> salida.put(String.valueOf(clave), sanear(contenido)));
            return Collections.unmodifiableMap(salida);
        }
        if (valor instanceof List<?> lista) {
            return lista.stream().map(this::sanear).toList();
        }
        return valor;
    }

    private String ocultar(String valor) {
        if (valor == null) {
            return null;
        }
        return FRAGMENTO.matcher(COMPLETO.matcher(valor).replaceAll(ENMASCARADO))
                .replaceAll(ENMASCARADO);
    }
}
