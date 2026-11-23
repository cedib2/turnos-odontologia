package com.turnosodontologia.depositpayments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

/** Wiring de C-15: solo RestTemplate y javax.crypto ya presentes en el stack (MP-2/MP-10). */
@Configuration
public class MercadoPagoConfig {

    @Bean
    WebhookSignatureVerifier webhookSignatureVerifier(
            @Value("${MERCADOPAGO_WEBHOOK_SECRET}") String secreto) {
        return new WebhookSignatureVerifier(secreto);
    }

    @Bean
    RestTemplate restTemplateParaMercadoPago() {
        return new RestTemplate();
    }

    @Bean
    MercadoPagoPort mercadoPagoPort(RestTemplate restTemplateParaMercadoPago,
                                    @Value("${APP_BASE_URL}") String baseUrl,
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
