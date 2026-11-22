package com.turnosodontologia.application;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentActorProviderTest {

    @Test
    void devuelveActorFijoRecepcionistaEnModoSimulado() {
        CurrentActorProvider provider = new SimulatedCurrentActorProvider();

        ActorSimulado actor = provider.current();

        assertEquals("recepcionista", actor.nombre());
        assertEquals("SIMULADO", actor.modo());
    }

    @Test
    void exponeActorSystemParaTareasInternasSinAutenticacion() {
        CurrentActorProvider provider = new SimulatedCurrentActorProvider();

        ActorSimulado actor = provider.system();

        assertEquals("system", actor.nombre());
        assertEquals("SIMULADO", actor.modo());
    }

    @Test
    void noLeeIdentidadDeLaRequestNiDefineRutasDeAutenticacion() throws IOException {
        List<String> prohibidos = List.of(
                "RequestHeader", "CookieValue", "HttpHeaders", "SecurityContext",
                "spring-security", "/auth");
        List<String> hallazgos = new ArrayList<>();
        List<Path> raices = List.of(Path.of("src", "main", "java"),
                Path.of("src", "main", "resources"));
        Path paqueteAuth = Path.of("src", "main", "java", "com", "turnosodontologia", "auth")
                .toAbsolutePath();

        for (Path raiz : raices) {
            if (!Files.exists(raiz)) {
                continue;
            }
            try (var paths = Files.walk(raiz)) {
                for (Path path : paths.filter(Files::isRegularFile)
                        .filter(ruta -> !ruta.toAbsolutePath().startsWith(paqueteAuth)).toList()) {
                    String contenido = Files.readString(path);
                    for (String prohibido : prohibidos) {
                        if (contenido.contains(prohibido)) {
                            hallazgos.add(path + " -> " + prohibido);
                        }
                    }
                }
            }
        }

        assertEquals(List.of(), hallazgos);
    }
}
