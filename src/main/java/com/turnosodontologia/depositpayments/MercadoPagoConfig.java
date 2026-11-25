package com.turnosodontologia.depositpayments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

/**
 * Wiring de C-15: solo RestTemplate y javax.crypto ya presentes en el stack (MP-2/MP-10).
 * Los defaults son valores de prueba obvios, nunca credenciales reales: evitan
 * PlaceholderResolutionException cuando el contexto no trae el perfil `test` ni
 * variables de entorno (CI). Los valores reales se inyectan por entorno/perfil.
 */
@Configuration
public class MercadoPagoConfig {

    @Bean
    WebhookSignatureVerifier webhookSignatureVerifier(
            @Value("${MERCADOPAGO_WEBHOOK_SECRET:secreto-de-prueba-no-real}") String secreto) {
        return new WebhookSignatureVerifier(secreto);
    }

    @Bean
    RestTemplate restTemplateParaMercadoPago() {
        return new RestTemplate();
    }

    @Bean
    MercadoPagoPort mercadoPagoPort(RestTemplate restTemplateParaMercadoPago,
                                    @Value("${APP_BASE_URL:http://localhost:8080}") String baseUrl,
                                    @Value("${MERCADOPAGO_ACCESS_TOKEN:}") String accessToken) {
        return new MercadoPagoRestAdapter(restTemplateParaMercadoPago, baseUrl, accessToken);
    }

    @Bean
    MercadoPagoIdempotencyStore mercadoPagoIdempotencyStore(DataSource dataSource) {
        return new MercadoPagoIdempotencyStore(dataSource);
    }

    @Bean
    NotificacionPagoPort notificacionPagoPort() {
        return new NotificacionPagoSimulada();
    }
}
