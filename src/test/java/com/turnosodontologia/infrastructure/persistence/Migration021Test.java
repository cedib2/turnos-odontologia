package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Migration021Test extends PostgresFlywayTest {

    @Test
    void declaraSeniasConImporteMonedaEstadosUnicidadYSinContabilidad() {
        for (String columna : List.of("id", "turno_id", "presupuesto_id", "monto", "moneda",
                "estado", "proveedor", "external_reference", "idempotency_key",
                "fecha_aprobacion", "conciliado", "activo", "created_at", "created_by",
                "updated_at", "updated_by")) {
            assertEquals(1, countColumn("senias", columna), "falta senias." + columna);
        }

        assertEquals("numeric", tipoColumna("senias", "monto"));
        assertEquals("character varying", tipoColumna("senias", "estado"));
        assertEquals("YES", nullable("senias", "turno_id"));
        assertEquals("YES", nullable("senias", "presupuesto_id"));
        assertEquals("YES", nullable("senias", "fecha_aprobacion"));

        for (String indice : List.of("uq_senias_external_reference", "uq_senias_idempotency_key",
                "ix_senias_turno", "ix_senias_presupuesto", "ix_senias_estado")) {
            assertEquals(1, countIndice(indice), "falta el índice " + indice);
        }

        Integer enums = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_type WHERE typcategory = 'E' AND typname LIKE 'senia%'",
                Integer.class);
        assertEquals(0, enums);

        // V020 permanece intacta y no se inventa contabilidad (MP-3).
        for (String columna : List.of("id", "paciente_id", "turno_id", "total", "moneda")) {
            assertEquals(1, countColumn("presupuestos", columna), "V020 quedó alterada: " + columna);
        }
        for (String tabla : List.of("caja", "asientos", "asiento", "arqueo", "arqueos")) {
            assertEquals(0, countTabla(tabla), "no se debe crear la tabla " + tabla);
        }
        for (String columna : List.of("comprobante", "impuesto", "asiento_id", "caja_id")) {
            assertEquals(0, countColumn("senias", columna), "senias no debe tener " + columna);
        }
    }

    @Test
    void rechazaMontosMonedasEstadosProveedoresYReferenciasFueraDelContrato() {
        assertThrows(DataIntegrityViolationException.class, () -> insertar(
                "S-021-1", "S-021-K1", -1, "ARS", "pendiente", "mercadopago"));
        assertThrows(DataIntegrityViolationException.class, () -> insertar(
                "S-021-2", "S-021-K2", 10, "USD", "pendiente", "mercadopago"));
        assertThrows(DataIntegrityViolationException.class, () -> insertar(
                "S-021-3", "S-021-K3", 10, "ARS", "borrador", "mercadopago"));
        assertThrows(DataIntegrityViolationException.class, () -> insertar(
                "S-021-4", "S-021-K4", 10, "ARS", "pendiente", "otro"));
        assertThrows(DataIntegrityViolationException.class, () -> insertar(
                "S-021-5", "S-021-K5", 10, "ARS", "pendiente", "mercadopago", "  "));
        assertThrows(DataIntegrityViolationException.class, () -> insertar(
                "S-021-5b", "S-021-K5b", 10, "ARS", "pendiente", "mercadopago", ""));

        insertar("S-021-6", "S-021-K6", 10, "ARS", "pendiente", "mercadopago");
        assertThrows(DataIntegrityViolationException.class, () -> insertar(
                "S-021-6", "S-021-K7", 10, "ARS", "pendiente", "mercadopago"));
        assertThrows(DataIntegrityViolationException.class, () -> insertar(
                "S-021-7", "S-021-K6", 10, "ARS", "pendiente", "mercadopago"));

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM senias", Integer.class);
        assertEquals(1, total);
    }

    private void insertar(String referencia, String clave, int monto, String moneda,
                          String estado, String proveedor) {
        insertar(referencia, clave, monto, moneda, estado, proveedor, "senia-" + referencia);
    }

    private void insertar(String referencia, String clave, int monto, String moneda,
                          String estado, String proveedor, String externalReference) {
        jdbcTemplate.update("INSERT INTO senias (external_reference, idempotency_key, monto, "
                        + "moneda, estado, proveedor) VALUES (?, ?, ?, ?, ?, ?)",
                externalReference, clave, monto, moneda, estado, proveedor);
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
