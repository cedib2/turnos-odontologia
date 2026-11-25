package com.turnosodontologia.invoicing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Doble del frente ARCA: por defecto simula éxito y permite forzar un error. */
@TestConfiguration
public class FacturacionTestConfig {

    @Bean
    @Primary
    FacturacionStub facturacionStub() {
        return new FacturacionStub();
    }

    public static final class FacturacionStub implements FacturacionPort {

        private final FacturacionSimulada simulada = new FacturacionSimulada();
        private String fallo;

        @Override
        public FacturacionResultado emitir(SolicitudFacturacion solicitud) {
            if (fallo != null) {
                String mensaje = fallo;
                fallo = null;
                return FacturacionResultado.rechazado(mensaje);
            }
            return simulada.emitir(solicitud);
        }

        public void fallarSiguienteCon(String mensaje) {
            this.fallo = mensaje;
        }

        public void reset() {
            this.fallo = null;
        }
    }
}
