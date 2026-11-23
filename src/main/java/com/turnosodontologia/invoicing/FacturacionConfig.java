package com.turnosodontologia.invoicing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

/**
 * Wiring de C-16. Los defaults son valores de prueba obvios, nunca credenciales
 * reales: evitan PlaceholderResolutionException en CI sin variables ARCA.
 * El adaptador REST solo existe fuera de dev/test; en esos perfiles se usa el
 * simulado y no se realiza ninguna llamada de red (AR-7, AR-8).
 */
@Configuration
public class FacturacionConfig {

    @Bean
    @Profile({"dev", "test"})
    FacturacionPort facturacionSimulada() {
        return new FacturacionSimulada();
    }

    @Bean
    @Profile("!test & !dev")
    RestTemplate restTemplateParaARCA() {
        return new RestTemplate();
    }

    @Bean
    @Profile("!test & !dev")
    FacturacionPort facturacionRest(RestTemplate restTemplateParaARCA,
                                    @Value("${ARCA_BASE_URL:http://localhost:9999/arca}") String baseUrl,
                                    @Value("${ARCA_ACCESS_TOKEN:}") String accessToken) {
        return new FacturacionRestAdapter(restTemplateParaARCA, baseUrl, accessToken);
    }

    @Bean
    FacturacionIdempotencyStore facturacionIdempotencyStore(DataSource dataSource) {
        return new FacturacionIdempotencyStore(dataSource);
    }

    @Bean
    AvisoRecepcionPort avisoRecepcionPort() {
        return new AvisoRecepcionSimulado();
    }
}
