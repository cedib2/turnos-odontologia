package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Migration005Test extends PostgresFlywayTest {

    @Test
    void declaraTratamientosHorariosFeriadosYZonaHoraria() {
        Integer columnas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'tratamientos' "
                        + "AND column_name IN ('codigo', 'nombre', 'duracion_minutos')", Integer.class);
        assertEquals(3, columnas);

        assertEquals(1, countTabla("horarios"));
        assertEquals(1, countTabla("feriados"));
        String zona = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'horarios' "
                        + "AND column_name = 'zona_horaria'", String.class);
        assertTrue(zona.contains("America/Argentina/Buenos_Aires"));
    }

    @Test
    void laBaseRechazaDuracionNoPositivaYSemillaNoInventada() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO tratamientos (codigo, nombre, duracion_minutos) VALUES (?, ?, ?)",
                "INVALIDA", "Duración cero", 0));

        Integer tratamientos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tratamientos", Integer.class);
        assertEquals(0, tratamientos);
    }

    private int countTabla(String tabla) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?", Integer.class, tabla);
    }
}
