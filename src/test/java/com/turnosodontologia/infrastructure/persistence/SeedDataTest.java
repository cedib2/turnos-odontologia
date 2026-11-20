package com.turnosodontologia.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedDataTest {

    private static final Path MIGRATION =
            Path.of("src/main/resources/db/migration/V001__core_models.sql");

    private String sql() throws IOException {
        return Files.readString(MIGRATION, StandardCharsets.UTF_8);
    }

    private List<int[]> permisosDelRol(String sql, int rolId) {
        Pattern fila = Pattern.compile("\\((\\d+), (\\d+)\\)");
        Matcher matcher = fila.matcher(sql.substring(sql.indexOf("INSERT INTO rol_permiso")));
        return matcher.results()
                .map(result -> new int[]{Integer.parseInt(result.group(1)), Integer.parseInt(result.group(2))})
                .filter(par -> par[0] == rolId)
                .toList();
    }

    @Test
    void auditorTieneSoloPermisosDeLecturaYPacienteNoTieneCuenta() throws IOException {
        String sql = sql();

        List<int[]> permisosAuditor = permisosDelRol(sql, 5);
        assertEquals(2, permisosAuditor.size());
        assertTrue(permisosAuditor.stream().allMatch(par -> par[1] == 10 || par[1] == 11));
        assertFalse(sql.contains("INSERT INTO usuarios"), "el rol paciente no lleva cuenta de usuario");
    }

    @Test
    void siembraCincoRolesUnConsultorioYDifiereDireccion() throws IOException {
        String sql = sql();

        for (String codigo : List.of("paciente", "recepcionista", "odontologo", "administrador", "auditor")) {
            assertTrue(sql.contains("'" + codigo + "'"), "falta el rol " + codigo);
        }
        assertTrue(sql.contains("INSERT INTO consultorios (id, nombre) VALUES (1, 'Consultorio principal');"));
        assertFalse(sql.contains("'direccion'"), "dirección queda diferida con C-19");
    }
}
