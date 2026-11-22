package com.turnosodontologia.publictoken;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PublicTokenLogSanitizerTest {

    @Test
    void tokenYFragmentosNoAparecenEnLogsNiEnEventosSimulados() {
        Logger logger = (Logger) LoggerFactory.getLogger("publictoken-log-test");
        logger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            Instant ahora = Instant.parse("2026-11-21T12:00:00Z");
            Clock reloj = Clock.fixed(ahora, ZoneOffset.UTC);
            PublicTokenSigner firmador = new PublicTokenSigner(
                    PublicTokenTestSupport.SEMILLA, PublicTokenTestSupport.EMISOR);
            PublicTokenTestSupport.FakeMetadataStore metadatos = new PublicTokenTestSupport.FakeMetadataStore();
            InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();
            PublicTokenSanitizer sanitizador = new PublicTokenSanitizer();
            PublicTokenService servicio = new PublicTokenService(firmador, metadatos, eventos, reloj, sanitizador);

            PublicTokenClaims validas = PublicTokenTestSupport.claims(
                    "jti-valido-11111111-2222-3333-4444-555555555555",
                    PublicTokenScope.RESERVAR, ahora, null, "paciente-1");
            PublicTokenClaims vencidas = PublicTokenTestSupport.claims(
                    "jti-vencido-11111111-2222-3333-4444-555555555555",
                    PublicTokenScope.RESERVAR, ahora.minusSeconds(7200), null, "paciente-2");
            PublicTokenClaims rechazadas = PublicTokenTestSupport.claims(
                    "jti-rechazado-11111111-2222-3333-4444-555555555555",
                    PublicTokenScope.HISTORIA, ahora, null, "paciente-3");

            String tokenValido = servicio.emitir(validas);
            String tokenVencido = servicio.emitir(vencidas);
            String tokenForjado = forjar(tokenValido);
            String tokenRechazado = servicio.emitir(rechazadas);

            registrar(logger, sanitizador, "valida", tokenValido, validas.jti(),
                    servicio.validar(tokenValido).motivo());
            registrar(logger, sanitizador, "vencida", tokenVencido, vencidas.jti(),
                    servicio.validar(tokenVencido).motivo());
            registrar(logger, sanitizador, "forjada", tokenForjado, "",
                    servicio.validar(tokenForjado).motivo());
            registrar(logger, sanitizador, "rechazada", tokenRechazado, rechazadas.jti(),
                    servicio.validar(tokenRechazado).motivo());

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            sinFragmentos(logs, tokenValido, validas.jti());
            sinFragmentos(logs, tokenVencido, vencidas.jti());
            sinFragmentos(logs, tokenForjado, "");
            sinFragmentos(logs, tokenRechazado, rechazadas.jti());

            String serializacionEventos = eventos.eventos().stream()
                    .map(evento -> evento.motivo() + evento.metadata())
                    .collect(Collectors.joining("\n"));
            sinFragmentos(serializacionEventos, tokenRechazado, rechazadas.jti());
        } finally {
            logger.detachAppender(appender);
        }
    }

    private void registrar(Logger logger, PublicTokenSanitizer sanitizador, String operacion,
                           String token, String jti, String motivo) {
        logger.info("operacion={} uri={} detalle={}", operacion,
                sanitizador.uri("/publico/reservar?token=" + token),
                sanitizador.mensaje("motivo=" + motivo + " token=" + token + " jti=" + jti));
    }

    private void sinFragmentos(String texto, String token, String jti) {
        assertFalse(texto.contains(token), "token completo filtrado");
        for (String parte : token.split("\\.")) {
            if (parte.length() >= 20) {
                assertFalse(texto.contains(parte), "fragmento identificable filtrado");
            }
        }
        if (!jti.isEmpty()) {
            assertFalse(texto.contains(jti), "jti filtrado");
        }
    }

    private String forjar(String token) {
        char ultimo = token.charAt(token.length() - 1);
        return token.substring(0, token.length() - 1) + (ultimo == 'A' ? 'B' : 'A');
    }
}
