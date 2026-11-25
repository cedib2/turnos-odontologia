package com.turnosodontologia.invoicing;

import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AR-7, AR-8, AR-14: arranque sin secretos y adaptador simulado en el perfil test. */
class FacturacionConfigTest extends PostgresFlywayTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    void elPerfilTestUsaElAdaptadorSimuladoYNingunAdaptadorRest() {
        assertInstanceOf(FacturacionSimulada.class, contexto.getBean(FacturacionPort.class));
        assertEquals(0, contexto.getBeansOfType(FacturacionRestAdapter.class).size());
    }

    @Test
    void caeDeterministaMarcadoComoPruebaYSinRed() {
        SolicitudFacturacion solicitud = new SolicitudFacturacion("factura", "senia-77",
                "Seña odontológica", new java.math.BigDecimal("100.00"), "ARS", "C", "0001");
        FacturacionResultado primero = new FacturacionSimulada().emitir(solicitud);
        FacturacionResultado segundo = new FacturacionSimulada().emitir(solicitud);

        assertEquals(primero.cae(), segundo.cae());
        assertTrue(FacturacionSimulada.esCaeDePrueba(primero.cae()));
    }

    @Test
    void nombresSinValoresYDominioTestSinSecretosNiSdk() throws Exception {
        String env = Files.readString(Path.of(".env.example"));
        String perfil = Files.readString(Path.of("src/test/resources/application-test.yml"));
        String pom = Files.readString(Path.of("pom.xml")).toLowerCase();

        for (String nombre : List.of("ARCA_BASE_URL", "ARCA_ACCESS_TOKEN")) {
            assertTrue(env.contains(nombre), ".env.example debe declarar " + nombre);
            assertTrue(perfil.contains(nombre), "application-test.yml debe resolver " + nombre);
        }
        assertFalse(perfil.contains("APP_USR-"));
        assertFalse(perfil.contains("TEST-"));
        assertFalse(pom.contains("arca"), "sin SDK ni dependencia nueva");
    }
}
