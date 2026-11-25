package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** W-2: V023 aplica limpia, V001-V022 quedan intactas y no aparece contabilidad ni notificación. */
class Migration023Test extends PostgresFlywayTest {

    @Test
    void aplicaV023SobreV001AV022SinTocarContratosArchivados() {
        Set<String> versiones = Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion().getVersion())
                .collect(Collectors.toSet());
        assertEquals(Set.of("001", "002", "003", "004", "005", "006", "010", "011", "012", "013",
                "020", "021", "022", "023", "030", "031", "032"), versiones);

        for (String tabla : List.of("turnos", "ocupaciones", "senias", "public_idempotencia")) {
            assertEquals(1, countTabla(tabla), "V001-V022 quedó alterada: " + tabla);
        }
        for (String columna : List.of("operacion", "idempotency_key_hash", "creado_en")) {
            assertEquals(1, countColumn("public_idempotencia", columna), columna);
        }
        for (String columna : List.of("id", "monto", "moneda", "estado", "conciliado")) {
            assertEquals(1, countColumn("senias", columna), columna);
        }
    }

    @Test
    void declaraListaEsperaConEstadosDepositoOpcionalYAuditoria() {
        for (String columna : List.of("id", "candidato_id", "profesional_id", "sillon_id",
                "inicio_deseado", "fin_deseado", "deposito_requerido", "senia_id", "estado",
                "ofrecida_en", "turno_id", "created_at", "created_by", "updated_at",
                "updated_by", "activo")) {
            assertEquals(1, countColumn("lista_espera", columna), "falta lista_espera." + columna);
        }
        assertEquals("YES", nullable("lista_espera", "senia_id"));
        assertEquals("NO", nullable("lista_espera", "deposito_requerido"));
        assertEquals("bigint", tipoColumna("lista_espera", "candidato_id"));

        for (String indice : List.of("pk_lista_espera", "uq_lista_espera_candidato_rango",
                "ix_lista_espera_elegibles", "ix_lista_espera_sillon",
                "ix_lista_espera_senia", "ix_lista_espera_turno")) {
            assertEquals(1, countIndice(indice), "falta el índice " + indice);
        }
        assertEquals(1, countCheck("ck_lista_espera_estado"));
        assertEquals(1, countCheck("ck_lista_espera_rango"));
    }

    @Test
    void declaraMetricasConMetodologiaObligatoriaYPeriodoYYYYMM() {
        for (String columna : List.of("id", "sillon_id", "periodo", "ocupacion", "ausentismo",
                "metodologia", "calculada_en", "created_at", "created_by", "updated_at",
                "updated_by", "activo")) {
            assertEquals(1, countColumn("metricas_ocupacion", columna),
                    "falta metricas_ocupacion." + columna);
        }
        assertEquals("NO", nullable("metricas_ocupacion", "metodologia"));
        assertEquals("NO", nullable("metricas_ocupacion", "periodo"));
        for (String indice : List.of("pk_metricas_ocupacion", "uq_metricas_sillon_periodo",
                "ix_metricas_periodo")) {
            assertEquals(1, countIndice(indice), "falta el índice " + indice);
        }
        assertEquals(1, countCheck("ck_metricas_metodologia"));
        assertEquals(1, countCheck("ck_metricas_periodo"));
    }

    @Test
    void noCreaCajaAsientoArqueoNotificacionNiMetas() throws java.io.IOException {
        for (String tabla : List.of("caja", "asientos", "asiento", "arqueo", "arqueos",
                "metas", "objetivos")) {
            assertEquals(0, countTabla(tabla), "no se debe crear la tabla " + tabla);
        }
        // La tabla notificaciones la crea V010 (C-10); V023 no debe crearla.
        String v023 = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/db/migration/V023__waitlist_and_metrics.sql")).toLowerCase();
        assertEquals(false, v023.matches("(?s).*create\\s+table\\s+(if\\s+not\\s+exists\\s+)?notificacion.*"),
                "V023 no debe crear la tabla notificaciones");
        for (String columna : List.of("asiento_id", "caja_id", "arqueo_id", "meta", "objetivo")) {
            assertEquals(0, countColumn("metricas_ocupacion", columna), columna);
        }
    }

    private int countTabla(String tabla) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?", Integer.class, tabla);
    }

    private int countColumn(String tabla, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                Integer.class, tabla, columna);
    }

    private int countIndice(String indice) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
                Integer.class, indice);
    }

    private int countCheck(String constraint) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?", Integer.class, constraint);
    }

    private String nullable(String tabla, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                String.class, tabla, columna);
    }

    private String tipoColumna(String tabla, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                String.class, tabla, columna);
    }
}
