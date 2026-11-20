package com.turnosodontologia.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Migration001Test {

    private static final Path MIGRATION =
            Path.of("src/main/resources/db/migration/V001__core_models.sql");

    private String sql() throws IOException {
        return Files.readString(MIGRATION, StandardCharsets.UTF_8);
    }

    @Test
    void declaraLasSieteTablasConNombresFisicosYAuditoria() throws IOException {
        String sql = sql();

        for (String tabla : List.of("pacientes", "profesionales", "sillones", "consultorios",
                "usuarios", "roles", "permisos", "rol_permiso")) {
            assertTrue(sql.contains("CREATE TABLE " + tabla + " ("), "falta la tabla " + tabla);
        }
        for (String columna : List.of("created_at TIMESTAMPTZ", "created_by VARCHAR",
                "updated_at TIMESTAMPTZ", "updated_by VARCHAR", "activo BOOLEAN")) {
            assertTrue(sql.contains(columna), "falta la columna de auditoría " + columna);
        }
        assertTrue(sql.contains("CREATE SEQUENCE"), "los IDs deben usar secuencias BIGINT");
        assertTrue(sql.contains("id BIGINT"), "los IDs deben ser BIGINT");
    }

    @Test
    void declaraIndicesParcialesUnicidadYTiposDeFechaSinAntisolapamiento() throws IOException {
        String sql = sql();

        assertTrue(sql.contains("CREATE INDEX ix_pacientes_telefono ON pacientes (telefono);"));
        assertTrue(sql.contains("CREATE INDEX ix_profesionales_activo ON profesionales (activo) WHERE activo = true;"));
        assertTrue(sql.contains("CREATE INDEX ix_sillones_activo ON sillones (activo) WHERE activo = true;"));
        assertTrue(sql.contains("CREATE UNIQUE INDEX ux_pacientes_documento ON pacientes "
                + "(documento_tipo, documento_numero) WHERE activo = true;"));
        assertTrue(sql.contains("TIMESTAMPTZ"));
        assertFalse(sql.contains("deleted_at"), "v0.1 no usa borrado físico");
        assertFalse(sql.contains("sede_id"), "D3 descarta aislamiento por sede");
        assertFalse(sql.toLowerCase().contains("create table turnos"), "RN-AG-01 va en C-08");
        assertFalse(sql.contains("EXCLUDE USING"), "la exclusion constraint va en C-08");
        assertFalse(sql.contains("btree_gist"), "la extensión va en C-08");
    }
}
