package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Migration020Test extends PostgresFlywayTest {

    @Test
    void declaraPresupuestosEItemsConNumerosChecksIndicesYSinEnum() {
        for (String columna : List.of("id", "paciente_id", "turno_id", "estado", "moneda",
                "total", "activo", "created_at", "created_by", "updated_at", "updated_by")) {
            assertEquals(1, countColumn("presupuestos", columna), "falta presupuestos." + columna);
        }
        for (String columna : List.of("id", "presupuesto_id", "tratamiento_id", "cantidad",
                "precio", "descripcion", "activo")) {
            assertEquals(1, countColumn("items_presupuesto", columna), "falta items_presupuesto." + columna);
        }

        assertEquals("numeric", tipoColumna("presupuestos", "total"));
        assertEquals("numeric", tipoColumna("items_presupuesto", "precio"));
        assertEquals("YES", nullable("presupuestos", "turno_id"));
        assertEquals("character varying", tipoColumna("presupuestos", "estado"));

        for (String indice : List.of("ix_presupuestos_paciente", "ix_presupuestos_turno",
                "ix_items_presupuesto_presupuesto")) {
            assertEquals(1, countIndice(indice), "falta el índice " + indice);
        }

        Integer enums = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_type WHERE typcategory = 'E' AND typname LIKE 'presupuesto%'",
                Integer.class);
        assertEquals(0, enums);

        for (String tabla : List.of("presupuestos", "items_presupuesto")) {
            for (String prohibida : List.of("pago_id", "monto", "external_reference", "conciliado",
                    "comprobante", "impuesto", "asiento_id", "estado_pago")) {
                assertEquals(0, countColumn(tabla, prohibida), tabla + " no debe tener " + prohibida);
            }
        }
    }

    @Test
    void rechazaEstadosMonedasYCantidadesFueraDelContrato() {
        long paciente = crearPaciente();
        long tratamiento = crearTratamiento();
        long presupuesto = crearPresupuesto(paciente);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO presupuestos (paciente_id, estado, moneda, total) VALUES (?, 'borrador', 'ARS', 1)",
                paciente));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO presupuestos (paciente_id, estado, moneda, total) VALUES (?, 'emitido', 'USD', 1)",
                paciente));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO presupuestos (paciente_id, estado, moneda, total) VALUES (?, 'emitido', 'ARS', -1)",
                paciente));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO items_presupuesto (presupuesto_id, tratamiento_id, cantidad, precio, descripcion) "
                        + "VALUES (?, ?, 0, 1, 'item')", presupuesto, tratamiento));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO items_presupuesto (presupuesto_id, tratamiento_id, cantidad, precio, descripcion) "
                        + "VALUES (?, ?, 1, -1, 'item')", presupuesto, tratamiento));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO items_presupuesto (presupuesto_id, tratamiento_id, cantidad, precio, descripcion) "
                        + "VALUES (?, ?, 1, 1, '  ')", presupuesto, tratamiento));

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM presupuestos", Integer.class);
        assertEquals(1, total);
    }

    private int countColumn(String tabla, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                Integer.class, tabla, columna);
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

    private long crearPaciente() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO pacientes (documento_tipo, documento_numero, nombre, apellido, telefono) "
                        + "VALUES ('DNI', 'MIG-020', 'Fixture', 'Paciente', '555') RETURNING id",
                Long.class);
    }

    private long crearTratamiento() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO tratamientos (codigo, nombre, duracion_minutos) VALUES ('MIG-020', 'Fixture', 30) "
                        + "RETURNING id", Long.class);
    }

    private long crearPresupuesto(long paciente) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO presupuestos (paciente_id, estado, moneda, total) "
                        + "VALUES (?, 'emitido', 'ARS', 10.00) RETURNING id", Long.class, paciente);
    }
}
