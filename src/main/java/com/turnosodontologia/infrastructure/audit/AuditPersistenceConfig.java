package com.turnosodontologia.infrastructure.audit;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.audit.AuditoriaCanonicalizer;
import com.turnosodontologia.audit.AuditoriaCatalogo;
import com.turnosodontologia.audit.AuditoriaMetadataValidator;
import com.turnosodontologia.audit.JdbcAuditEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!test")
public class AuditPersistenceConfig {

    @Bean
    public AuditEventPublisher auditEventPublisher(JdbcTemplate jdbcTemplate) {
        return new JdbcAuditEventPublisher(jdbcTemplate, new AuditoriaCanonicalizer(),
                new AuditoriaCatalogo(), new AuditoriaMetadataValidator());
    }
}
