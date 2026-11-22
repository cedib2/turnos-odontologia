package com.turnosodontologia.publictoken;

import com.turnosodontologia.interfaces.web.AgendaController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicAgendaConsumptionTest {

    @Test
    void tokenVigentePorPathOQuerySeValidaSinCredencialesDeStaff() throws Exception {
        Instant ahora = Instant.parse("2026-11-21T12:00:00Z");
        PublicTokenSigner firmador = new PublicTokenSigner(
                PublicTokenTestSupport.SEMILLA, PublicTokenTestSupport.EMISOR);
        PublicTokenService servicio = new PublicTokenService(firmador,
                new PublicTokenTestSupport.FakeMetadataStore(),
                new com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder(),
                PublicTokenTestSupport.reloj(ahora));
        String token = servicio.emitir(PublicTokenTestSupport.claims(
                "jti-publico-11111111-2222-3333-4444-555555555555",
                PublicTokenScope.RESERVAR, ahora, null, "paciente-1"));

        PublicAgendaConsumption consumo = new PublicAgendaConsumption(servicio);
        PublicAgendaConsumption.Resultado porPath = consumo.validarDesdePath(token);
        PublicAgendaConsumption.Resultado porQuery = consumo.validarDesdeQuery(token);

        assertTrue(porPath.permitido());
        assertEquals(PublicTokenScope.RESERVAR, porPath.scope());
        assertTrue(porQuery.permitido());
        assertEquals(PublicTokenScope.RESERVAR, porQuery.scope());

        Constructor<?>[] constructores = PublicAgendaConsumption.class.getDeclaredConstructors();
        assertEquals(1, constructores.length);
        assertArrayEquals(new Class<?>[]{PublicTokenService.class}, constructores[0].getParameterTypes());

        String codigo = Files.readString(Path.of("src", "main", "java", "com", "turnosodontologia",
                "publictoken", "PublicAgendaConsumption.java"));
        for (String prohibido : List.of("RequestHeader", "HttpHeaders", "getHeader(", "Authorization",
                "Bearer ", "SecurityContext", "spring-security", "/auth", "Usuario", "Rol")) {
            assertFalse(codigo.contains(prohibido), "Prohibido en consumo público: " + prohibido);
        }

        assertEquals("/agenda", AgendaController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertNotNull(AgendaController.class.getMethod("consultar",
                Long.class, Long.class, Instant.class, Instant.class));
    }
}
