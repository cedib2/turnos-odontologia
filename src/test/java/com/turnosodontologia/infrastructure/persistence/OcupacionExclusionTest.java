package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OcupacionExclusionTest extends PostgresFlywayTest {

    private static final Instant BASE = Instant.parse("2026-12-01T13:00:00Z");

    @Test
    void rechazaSolapamientoDelMismoProfesionalYPermiteContiguos() {
        long profesional = crearProfesional("MP-OCC-1");
        long sillon = crearSillon("S-OCC-1");
        long tratamiento = crearTratamiento("TRAT-OCC-1");
        long paciente = crearPaciente("OCC-1");

        long primero = crearTurno(paciente, profesional, sillon, tratamiento,
                BASE, BASE.plusSeconds(3600));
        ocuparTurno(primero, profesional, sillon, BASE, BASE.plusSeconds(3600));

        Instant solapado = BASE.plusSeconds(1800);
        assertThrows(DataIntegrityViolationException.class, () -> {
            long segundo = crearTurno(paciente, profesional, sillon, tratamiento,
                    solapado, solapado.plusSeconds(3600));
            ocuparTurno(segundo, profesional, sillon, solapado, solapado.plusSeconds(3600));
        });

        Instant contiguo = BASE.plusSeconds(3600);
        long tercero = crearTurno(paciente, profesional, sillon, tratamiento,
                contiguo, contiguo.plusSeconds(3600));
        ocuparTurno(tercero, profesional, sillon, contiguo, contiguo.plusSeconds(3600));

        assertEquals(2, count("turnos"));
        assertEquals(2, count("ocupaciones"));
    }

    @Test
    void rechazaSolapamientoDelMismoSillonConProfesionalDistinto() {
        long profesionalUno = crearProfesional("MP-OCC-2A");
        long profesionalDos = crearProfesional("MP-OCC-2B");
        long sillon = crearSillon("S-OCC-2");
        long tratamiento = crearTratamiento("TRAT-OCC-2");
        long paciente = crearPaciente("OCC-2");

        long primero = crearTurno(paciente, profesionalUno, sillon, tratamiento,
                BASE, BASE.plusSeconds(3600));
        ocuparTurno(primero, profesionalUno, sillon, BASE, BASE.plusSeconds(3600));

        Instant solapado = BASE.plusSeconds(900);
        assertThrows(DataIntegrityViolationException.class, () -> {
            long segundo = crearTurno(paciente, profesionalDos, sillon, tratamiento,
                    solapado, solapado.plusSeconds(3600));
            ocuparTurno(segundo, profesionalDos, sillon, solapado, solapado.plusSeconds(3600));
        });
    }

    @Test
    void rechazaTurnoQueSolapaUnBloqueoDelMismoSillon() {
        long profesional = crearProfesional("MP-OCC-3");
        long sillon = crearSillon("S-OCC-3");
        long tratamiento = crearTratamiento("TRAT-OCC-3");
        long paciente = crearPaciente("OCC-3");

        long bloqueo = crearBloqueo(null, sillon, BASE, BASE.plusSeconds(7200));
        ocuparBloqueo(bloqueo, null, sillon, BASE, BASE.plusSeconds(7200));

        Instant solapado = BASE.plusSeconds(1800);
        long turno = crearTurno(paciente, profesional, sillon, tratamiento,
                solapado, solapado.plusSeconds(1800));
        assertThrows(DataIntegrityViolationException.class,
                () -> ocuparTurno(turno, profesional, sillon, solapado, solapado.plusSeconds(1800)));
    }

    @Test
    void rechazaBloqueoQueSolapaUnTurnoDelMismoProfesional() {
        long profesional = crearProfesional("MP-OCC-4");
        long sillon = crearSillon("S-OCC-4");
        long tratamiento = crearTratamiento("TRAT-OCC-4");
        long paciente = crearPaciente("OCC-4");

        long turno = crearTurno(paciente, profesional, sillon, tratamiento,
                BASE, BASE.plusSeconds(1800));
        ocuparTurno(turno, profesional, sillon, BASE, BASE.plusSeconds(1800));

        assertThrows(DataIntegrityViolationException.class, () -> {
            long bloqueo = crearBloqueo(profesional, sillon, BASE, BASE.plusSeconds(3600));
            ocuparBloqueo(bloqueo, profesional, sillon, BASE, BASE.plusSeconds(3600));
        });
    }

    private int count(String tabla) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
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

    private long crearPaciente(String documento) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO pacientes (documento_tipo, documento_numero, nombre, apellido, telefono) "
                        + "VALUES ('DNI', ?, 'Fixture', 'Paciente', '555') RETURNING id",
                Long.class, documento);
    }

    private long crearTurno(long paciente, long profesional, long sillon, long tratamiento,
                            Instant inicio, Instant fin) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO turnos (paciente_id, profesional_id, sillon_id, tipo_tratamiento_id, "
                        + "inicio, fin, duracion_minutos, estado, origen) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 30, 'fixture-estado', 'fixture-origen') RETURNING id",
                Long.class, paciente, profesional, sillon, tratamiento,
                Timestamp.from(inicio), Timestamp.from(fin));
    }

    private long crearBloqueo(Long profesional, Long sillon, Instant desde, Instant hasta) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO bloqueos (profesional_id, sillon_id, desde, hasta, motivo) "
                        + "VALUES (?, ?, ?, ?, 'fixture-motivo') RETURNING id",
                Long.class, profesional, sillon, Timestamp.from(desde), Timestamp.from(hasta));
    }

    private long ocuparTurno(long turnoId, long profesional, long sillon, Instant inicio, Instant fin) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO ocupaciones (turno_id, profesional_id, sillon_id, inicio, fin) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class, turnoId, profesional, sillon, Timestamp.from(inicio), Timestamp.from(fin));
    }

    private long ocuparBloqueo(long bloqueoId, Long profesional, Long sillon,
                               Instant inicio, Instant fin) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO ocupaciones (bloqueo_id, profesional_id, sillon_id, inicio, fin) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class, bloqueoId, profesional, sillon,
                Timestamp.from(inicio), Timestamp.from(fin));
    }
}
