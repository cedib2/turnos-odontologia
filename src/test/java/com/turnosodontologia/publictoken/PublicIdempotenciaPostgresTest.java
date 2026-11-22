package com.turnosodontologia.publictoken;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicIdempotenciaPostgresTest extends PostgresFlywayTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void mismaClaveEnMismaOperacionNoRepiteEfectoYPuedeReutilizarseEnOtraOperacion() {
        PublicIdempotencyStore store = new PublicIdempotencyStore(dataSource);
        String clave = "clave-idem-2026-11-21";

        assertTrue(store.registrar("turno.crear", clave));
        assertFalse(store.registrar("turno.crear", clave));
        assertTrue(store.registrar("turno.cancelar", clave));

        assertEquals(2, contarFilas());
        assertTrue(store.esRepeticion("turno.crear", clave));
        assertFalse(store.esRepeticion("turno.crear", "otra-clave-2026"));

        Integer claveEnClaro = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public_idempotencia WHERE idempotency_key_hash = ?",
                Integer.class, clave);
        assertEquals(0, claveEnClaro);
    }

    private int contarFilas() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public_idempotencia", Integer.class);
    }
}
