package com.turnosodontologia.waitlist;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.audit.AuditoriaCanonicalizer;
import com.turnosodontologia.audit.JdbcAuditEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/** Falla solo al publicar `waitlist.adjudicado`, para probar la reversión atómica de C-19. */
public class WaitlistFailingAuditConfig {

    @Bean
    @Primary
    AuditEventPublisher auditEventPublisherQueFalla(JdbcTemplate jdbcTemplate) {
        AuditEventPublisher real = new JdbcAuditEventPublisher(
                jdbcTemplate, new AuditoriaCanonicalizer());
        return event -> {
            real.publish(event);
            if ("waitlist.adjudicado".equals(event.tipo())) {
                throw new IllegalStateException("fallo forzado de auditoría");
            }
        };
    }
}
