package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** AR-2, AR-3, AR-14: V022 aplica limpia sin tocar V001-V021 ni inventar contabilidad. */
class Migration022Test extends PostgresFlywayTest {

    @Test
    void declaraComprobantesConUnicidadPorTipoYSinContabilidad() {
        for (String columna : List.of("id", "senia_id", "presupuesto_id", "turno_id", "tipo",
                "estado", "moneda", "monto", "concepto", "cae", "fecha_emision", "intentos",
                "ultimo_error", "conciliado", "activo", "created_at", "created_by",
                "updated_at", "updated_by")) {
            assertEquals(1, countColumn("comprobantes", columna), "falta comprobantes." + columna);
        }

        assertEquals("numeric", tipoColumna("comprobantes", "monto"));
        assertEquals("character varying", tipoColumna("comprobantes", "estado"));
        assertEquals("YES", nullable("comprobantes", "presupuesto_id"));
        assertEquals("YES", nullable("comprobantes", "turno_id"));
        assertEquals("NO", nullable("comprobantes", "senia_id"));

        for (String indice : List.of("uq_comprobantes_senia_tipo", "ix_comprobantes_senia",
                "ix_comprobantes_presupuesto", "ix_comprobantes_turno",
                "ix_comprobantes_estado", "ix_comprobantes_tipo")) {
            assertEquals(1, countIndice(indice), "falta el índice " + indice);
        }

        Integer enums = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_type WHERE typcategory = 'E' AND typname LIKE 'comprobante%'",
                Integer.class);
        assertEquals(0, enums);

        for (String tabla : List.of("caja", "asientos", "asiento", "arqueo", "arqueos",
                "renglones", "impuestos")) {
            assertEquals(0, countTabla(tabla), "no se debe crear la tabla " + tabla);
        }
        for (String tabla : List.of("senias", "presupuestos")) {
            for (String columna : List.of("comprobante_id", "cae", "impuesto", "asiento_id")) {
                assertEquals(0, countColumn(tabla, columna), tabla + " no debe tener " + columna);
            }
        }
    }

    @Test
    void aplicaV022DespuesDeV021ConElHistorialV001AV021Intacto() {
        Set<String> versiones = Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion().getVersion())
                .collect(Collectors.toSet());
        // El fixture de test aplica el subconjunto V001-V006, V020, V021 y V022.
        assertEquals(Set.of("001", "002", "003", "004", "005", "006", "020", "021", "022"),
                versiones);

        for (String columna : List.of("id", "monto", "moneda", "estado", "conciliado")) {
            assertEquals(1, countColumn("senias", columna), "V021 quedó alterada: " + columna);
        }
        for (String columna : List.of("id", "paciente_id", "total", "moneda")) {
            assertEquals(1, countColumn("presupuestos", columna), "V020 quedó alterada: " + columna);
        }
    }

    @Test
    void rechazaTipoEstadoMonedaMontoYDuplicadosPeroPermiteNotaDeCredito() {
        Long seniaId = crearSenia();

        assertThrows(DataIntegrityViolationException.class,
                () -> insertar(seniaId, "recibo", "pendiente", "ARS", 10));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertar(seniaId, "factura", "borrador", "ARS", 10));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertar(seniaId, "factura", "pendiente", "USD", 10));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertar(seniaId, "factura", "pendiente", "ARS", -1));

        insertar(seniaId, "factura", "pendiente", "ARS", 10);
        assertThrows(DataIntegrityViolationException.class,
                () -> insertar(seniaId, "factura", "pendiente", "ARS", 10));
        insertar(seniaId, "nota_credito", "pendiente", "ARS", 10);

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM comprobantes", Integer.class);
        assertEquals(2, total);
    }

    private Long crearSenia() {
        jdbcTemplate.update("INSERT INTO senias (external_reference, idempotency_key, monto, "
                        + "moneda, estado, proveedor) VALUES (?, ?, ?, 'ARS', 'aprobado', 'mercadopago')",
                "senia-v022-" + System.nanoTime(), "idem-v022-" + System.nanoTime(), 10);
        return jdbcTemplate.queryForObject("SELECT id FROM senias ORDER BY id DESC LIMIT 1", Long.class);
    }

    private void insertar(Long seniaId, String tipo, String estado, String moneda, int monto) {
        jdbcTemplate.update("INSERT INTO comprobantes (senia_id, tipo, estado, moneda, monto, concepto) "
                        + "VALUES (?, ?, ?, ?, ?, 'Seña odontológica')",
                seniaId, tipo, estado, moneda, monto);
    }

    private int countColumn(String tabla, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                Integer.class, tabla, columna);
    }

    private int countTabla(String tabla) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, tabla);
    }

    private String tipoColumna(String tabla, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                String.class, tabla, columna);
    }

    private String nullable(String tabla, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                String.class, tabla, columna);
    }

    private int countIndice(String indice) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
                Integer.class, indice);
    }
}
