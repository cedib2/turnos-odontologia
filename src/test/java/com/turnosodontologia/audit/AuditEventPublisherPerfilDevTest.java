package com.turnosodontologia.audit;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.infrastructure.audit.AuditPersistenceConfig;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AuditEventPublisherPerfilDevTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class AuditEventPublisherPerfilDevTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void perfilDevRegistraUnUnicoPublicadorJdbc() {
        Map<String, AuditEventPublisher> publicadores =
                context.getBeansOfType(AuditEventPublisher.class);

        assertEquals(1, publicadores.size());
        assertInstanceOf(JdbcAuditEventPublisher.class, publicadores.values().iterator().next());
        assertTrue(context.getBeansOfType(InMemoryAuditEventRecorder.class).isEmpty());
    }

    @SpringBootConfiguration
    @ComponentScan(basePackageClasses = AuditPersistenceConfig.class)
    static class Config {

        @Bean
        JdbcTemplate jdbcTemplate() {
            return Mockito.mock(JdbcTemplate.class);
        }
    }
}
