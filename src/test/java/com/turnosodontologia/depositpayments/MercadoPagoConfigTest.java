package com.turnosodontologia.depositpayments;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MP-10: solo nombres de variables, sin secretos reales ni dependencias nuevas. */
class MercadoPagoConfigTest {

    @Test
    void elPerfilDeTestUsaSoloLosNombresYaPresentesEnEnvExample() throws Exception {
        String env = Files.readString(Path.of(".env.example"));
        String perfil = Files.readString(Path.of("src/test/resources/application-test.yml"));
        String pom = Files.readString(Path.of("pom.xml"));

        for (String nombre : List.of("MERCADOPAGO_ACCESS_TOKEN",
                "MERCADOPAGO_WEBHOOK_SECRET", "APP_BASE_URL")) {
            assertTrue(env.contains(nombre), ".env.example debe declarar " + nombre);
            assertTrue(perfil.contains(nombre), "application-test.yml debe resolver " + nombre);
        }
        assertFalse(perfil.contains("APP_USR-"), "no se versionan tokens reales");
        assertFalse(perfil.contains("TEST-"), "no se versionan credenciales reales");
        assertFalse(pom.toLowerCase().contains("mercadopago"), "sin SDK nuevo");
    }
}
