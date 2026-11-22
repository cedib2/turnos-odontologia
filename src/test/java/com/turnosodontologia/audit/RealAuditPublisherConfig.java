package com.turnosodontologia.audit;

import com.turnosodontologia.application.AuditEventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@TestConfiguration
public class RealAuditPublisherConfig {

    @Bean
    @Primary
    AuditEventPublisher publisher(JdbcTemplate jdbcTemplate) {
        return new JdbcAuditEventPublisher(jdbcTemplate, new AuditoriaCanonicalizer());
    }
}
