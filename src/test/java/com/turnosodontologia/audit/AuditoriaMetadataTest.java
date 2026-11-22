package com.turnosodontologia.audit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditoriaMetadataTest {

    private final AuditoriaMetadataValidator validator = new AuditoriaMetadataValidator();

    @Test
    void metadataPermitida() {
        Map<String, Object> metadata = Map.of("origen", "test", "motivo", "control",
                "detalle_operacion", "alta");

        assertDoesNotThrow(() -> validator.validar(metadata));
        assertEquals(metadata, validator.validar(metadata));
    }

    @Test
    void metadataPermitidaDeC06() {
        Map<String, Object> metadata = Map.of("jti", "jti-1", "scope", "HISTORIA",
                "pacienteId", "paciente-42");

        assertDoesNotThrow(() -> validator.validar(metadata));
    }

    @Test
    void rechazaLas22ClavesProhibidasAgrupadas() {
        List<String> credenciales = List.of("password", "password_hash", "hash_password",
                "secret", "api_key", "access_token", "refresh_token", "token",
                "authorization", "jwt", "credentials");
        List<String> personales = List.of("dni", "documento_numero", "telefono", "email",
                "direccion", "fecha_nacimiento");
        List<String> clinicas = List.of("firma", "signature", "diagnostico",
                "historia_clinica", "ficha_clinica");

        assertEquals(22, credenciales.size() + personales.size() + clinicas.size());
        for (String clave : List.of(credenciales, personales, clinicas).stream()
                .flatMap(List::stream).toList()) {
            assertThrows(IllegalArgumentException.class,
                    () -> validator.validar(Map.of(clave, "valor")), clave);
        }
    }

    @Test
    void rechazaClaveProhibidaAnidada() {
        Map<String, Object> metadata = Map.of("detalle", Map.of("credenciales",
                Map.of("access_token", "secreto")));

        assertThrows(IllegalArgumentException.class, () -> validator.validar(metadata));
    }

    @Test
    void rechazaVariantesDeCasoAcentoYSeparadores() {
        for (String clave : List.of("EMAIL", "Teléfono", "historia-clinica", "FECHA_NACIMIENTO")) {
            assertThrows(IllegalArgumentException.class,
                    () -> validator.validar(Map.of(clave, "valor")), clave);
        }
    }

    @Test
    void rechazaListaAnidadaConClaveProhibida() {
        Map<String, Object> metadata = Map.of("detalles", List.of(
                Map.of("origen", "test"), Map.of("API-KEY", "valor")));

        assertThrows(IllegalArgumentException.class, () -> validator.validar(metadata));
    }

    @Test
    void rechazaVariantesSinGuionBajoNiGuionDeClavesProhibidas() {
        Map<String, Object> metadata = Map.of("accessToken", "abc", "Password-Hash", "def");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validar(metadata));
    }
}
