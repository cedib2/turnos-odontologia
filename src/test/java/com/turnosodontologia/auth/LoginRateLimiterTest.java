package com.turnosodontologia.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimiterTest {

    @Test
    void cincoFallosBloqueanYLaVentanaParteDelPrimerFallo() {
        RelojAjustable reloj = new RelojAjustable();
        LoginRateLimiter limite = new LoginRateLimiter(reloj, 5, 15);

        for (int i = 0; i < 5; i++) {
            limite.registrarFallo("203.0.113.10", "rate-uno@example.com");
        }

        LoginRateLimiter.Decision decision = limite.evaluar("203.0.113.10", "rate-uno@example.com");
        assertTrue(decision.bloqueado());
        assertTrue(decision.reintentarEnSegundos() > 0);
        assertTrue(decision.reintentarEnSegundos() <= 15 * 60);

        reloj.avanzar(Duration.ofMinutes(14));
        LoginRateLimiter.Decision antesDeLaVentana = limite.evaluar("203.0.113.10", "rate-uno@example.com");
        assertTrue(antesDeLaVentana.bloqueado());
        assertEquals(60, antesDeLaVentana.reintentarEnSegundos());

        reloj.avanzar(Duration.ofMinutes(1));
        assertFalse(limite.evaluar("203.0.113.10", "rate-uno@example.com").bloqueado());
    }

    @Test
    void loginExitosoReiniciaElContadorDeEsaCombinacion() {
        RelojAjustable reloj = new RelojAjustable();
        LoginRateLimiter limite = new LoginRateLimiter(reloj, 5, 15);

        for (int i = 0; i < 4; i++) {
            limite.registrarFallo("203.0.113.11", "rate-dos@example.com");
        }
        assertFalse(limite.evaluar("203.0.113.11", "rate-dos@example.com").bloqueado());

        limite.registrarExito("203.0.113.11", "rate-dos@example.com");

        for (int i = 0; i < 4; i++) {
            limite.registrarFallo("203.0.113.11", "rate-dos@example.com");
        }
        assertFalse(limite.evaluar("203.0.113.11", "rate-dos@example.com").bloqueado());

        limite.registrarFallo("203.0.113.11", "rate-dos@example.com");
        assertTrue(limite.evaluar("203.0.113.11", "rate-dos@example.com").bloqueado());
    }

    @Test
    void contadoresSeparadosPorIpYUsuarioSinHerencia() {
        RelojAjustable reloj = new RelojAjustable();
        LoginRateLimiter limite = new LoginRateLimiter(reloj, 5, 15);

        for (int i = 0; i < 5; i++) {
            limite.registrarFallo("203.0.113.12", "rate-tres@example.com");
        }

        assertTrue(limite.evaluar("203.0.113.12", "rate-tres@example.com").bloqueado());
        assertFalse(limite.evaluar("203.0.113.12", "otro@example.com").bloqueado());
        assertFalse(limite.evaluar("198.51.100.12", "rate-tres@example.com").bloqueado());

        for (int i = 0; i < 4; i++) {
            limite.registrarFallo("203.0.113.12", "otro@example.com");
            limite.registrarFallo("198.51.100.12", "rate-tres@example.com");
        }
        assertFalse(limite.evaluar("203.0.113.12", "otro@example.com").bloqueado());
        assertFalse(limite.evaluar("198.51.100.12", "rate-tres@example.com").bloqueado());
    }

    @Test
    void umbralYVentanaSonConfigurables() {
        RelojAjustable reloj = new RelojAjustable();
        LoginRateLimiter limite = new LoginRateLimiter(reloj, 3, 5);

        for (int i = 0; i < 3; i++) {
            limite.registrarFallo("192.0.2.10", "rate-cuatro@example.com");
        }
        assertTrue(limite.evaluar("192.0.2.10", "rate-cuatro@example.com").bloqueado());

        reloj.avanzar(Duration.ofMinutes(5));
        assertFalse(limite.evaluar("192.0.2.10", "rate-cuatro@example.com").bloqueado());
    }

    @Test
    void disenoYDecisionesDeclaranQueElLimiteNoEsDistribuido() throws IOException {
        String documentacion = Files.readString(Path.of("docs", "decisiones", "C-04-decisiones-auth-rbac.md"));

        for (String frase : List.of("en memoria", "no distribuido", "se esquiva rotando de IP",
                "no se comparte entre réplicas", "se pierde al reiniciar el proceso")) {
            assertTrue(documentacion.contains(frase), "Falta declaración: " + frase);
        }
    }

    private static final class RelojAjustable extends Clock {

        private Instant ahora = Instant.parse("2026-11-21T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        private void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }
    }
}
