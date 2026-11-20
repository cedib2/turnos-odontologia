package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Migration006Test extends PostgresFlywayTest {

    @Test
    void declaraTurnosYBloqueosConAuditoriaIndicesYCamposSemanticos() {
        assertEquals(1, countColumn("turnos", "token_publico"));
        assertEquals(1, countColumn("turnos", "es_sobreturno"));
        assertEquals(1, countColumn("turnos", "estado"));
        assertEquals(1, countColumn("turnos", "origen"));
        assertEquals(1, countColumn("bloqueos", "motivo"));
        assertEquals(1, countColumn("turnos", "activo"));

        String tipoEstado = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'turnos' "
                        + "AND column_name = 'estado'", String.class);
        assertEquals("character varying", tipoEstado);

        for (String indice : new String[]{"ix_turnos_profesional_inicio_fin",
                "ix_turnos_sillon_inicio_fin", "ix_turnos_estado", "ix_turnos_token_publico"}) {
            assertEquals(1, countIndice(indice), "falta el índice " + indice);
        }
    }

    @Test
    void rechazaEstadoYOrigenVaciosSinDefinirEnum() {
        long profesional = crearProfesional("MP-006");
        long sillon = crearSillon("S-006");
        long tratamiento = crearTratamiento("TRAT-006");
        long paciente = crearPaciente("006");

        assertThrows(DataIntegrityViolationException.class, () -> crearTurno(
                paciente, profesional, sillon, tratamiento, Instant.parse("2026-11-30T12:00:00Z"),
                "", "fixture-origen"));
        assertThrows(DataIntegrityViolationException.class, () -> crearTurno(
                paciente, profesional, sillon, tratamiento, Instant.parse("2026-11-30T13:00:00Z"),
                "fixture-estado", " "));

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM turnos", Integer.class);
        assertEquals(0, total);
        Integer enums = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_type WHERE typcategory = 'E' AND typname LIKE 'turnos_%'", Integer.class);
        assertEquals(0, enums);
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

    private long crearProfesional(String matricula) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO profesionales (matricula, nombre, apellido) VALUES (?, ?, ?) RETURNING id",
                Long.class, matricula, "Fixture", "Profesional");
    }

    private long crearSillon(String numero) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO sillones (numero, consultorio_id) VALUES (?, 1) RETURNING id",
                Long.class, numero);
    }

    private long crearTratamiento(String codigo) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO tratamientos (codigo, nombre, duracion_minutos) VALUES (?, ?, 30) RETURNING id",
                Long.class, codigo, "Fixture");
    }

    private long crearPaciente(String sufijo) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO pacientes (documento_tipo, documento_numero, nombre, apellido, telefono) "
                        + "VALUES ('DNI', ?, 'Fixture', 'Paciente', '555') RETURNING id",
                Long.class, sufijo);
    }

    private long crearTurno(long paciente, long profesional, long sillon, long tratamiento,
                            Instant inicio, String estado, String origen) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO turnos (paciente_id, profesional_id, sillon_id, tipo_tratamiento_id, "
                        + "inicio, fin, duracion_minutos, estado, origen) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 30, ?, ?) RETURNING id",
                Long.class, paciente, profesional, sillon, tratamiento, Timestamp.from(inicio),
                Timestamp.from(inicio.plusSeconds(1800)), estado, origen);
    }
}
