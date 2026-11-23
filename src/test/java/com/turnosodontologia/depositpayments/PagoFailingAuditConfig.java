package com.turnosodontologia.depositpayments;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.audit.AuditoriaCanonicalizer;
import com.turnosodontologia.audit.JdbcAuditEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/** Publica en la base real y falla después, para probar la reversión atómica. */
public class PagoFailingAuditConfig {

    @Bean
    @Primary
    AuditEventPublisher auditEventPublisherQueFalla(JdbcTemplate jdbcTemplate) {
        AuditEventPublisher real = new JdbcAuditEventPublisher(
                jdbcTemplate, new AuditoriaCanonicalizer());
        return event -> {
            real.publish(event);
            throw new IllegalStateException("fallo forzado de auditoría");
        };
    }

    @Bean
    @Primary
    MercadoPagoPort mercadoPagoPortStub() {
        return new PagoTestConfig.MercadoPagoStub();
    }
}
