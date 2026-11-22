package com.turnosodontologia.publictoken;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicDeltaInvariantsTest {

    @Test
    void deltaAgendaEsAditivoYGuardiasDeC04NoSeDebilitan() throws IOException {
        String agenda = Files.readString(Path.of("openspec", "specs", "agenda", "spec.md"));
        assertEquals(13, contar(agenda, "### Requirement:"));
        assertEquals(31, contar(agenda, "#### Scenario:"));
        assertTrue(agenda.contains("Delta puramente aditivo"));
        assertTrue(agenda.contains("Sin rutas de autenticación en la superficie de agenda"));

        String guardiaAgenda = Files.readString(Path.of("src", "test", "java", "com",
                "turnosodontologia", "interfaces", "web", "AgendaSurfaceTest.java"));
        assertTrue(guardiaAgenda.contains("SecurityContext"));
        assertTrue(guardiaAgenda.contains("@RequestHeader"));
        assertTrue(guardiaAgenda.contains("Bearer "));
        assertTrue(guardiaAgenda.contains("getHeader("));

        String guardiaActor = Files.readString(Path.of("src", "test", "java", "com",
                "turnosodontologia", "application", "CurrentActorProviderTest.java"));
        assertTrue(guardiaActor.contains("HttpHeaders"));
        assertTrue(guardiaActor.contains("spring-security"));
        assertTrue(guardiaActor.contains("/auth"));

        String guardiaAuditoria = Files.readString(Path.of("src", "test", "java", "com",
                "turnosodontologia", "application", "AuditEventPublisherTest.java"));
        assertTrue(guardiaAuditoria.contains("CREATE TABLE AUDIT"));
        assertTrue(guardiaAuditoria.contains("/audit"));

        String auditoria = Files.readString(Path.of("openspec", "specs", "audit-event-emission", "spec.md"));
        assertTrue(auditoria.contains("SIMULADO"));
        assertTrue(auditoria.contains("MUST NOT crear tabla append-only"));

        Path consumo = Path.of("src", "main", "java", "com", "turnosodontologia",
                "publictoken", "PublicAgendaConsumption.java");
        assertTrue(Files.exists(consumo), "falta el punto de consumo público");
        String codigo = Files.readString(consumo);
        assertFalse(codigo.contains("MODIFICAR_AGENDA"), "centinela de invariants");
    }

    private long contar(String contenido, String marcador) {
        return contenido.lines().filter(linea -> linea.startsWith(marcador)).count();
    }
}
