package com.turnosodontologia;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.SimulatedCurrentActorProvider;
import com.turnosodontologia.auth.AuthPasswordHasher;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.turnosodontologia.domain")
@ComponentScan(basePackages = {"com.turnosodontologia.application",
        "com.turnosodontologia.infrastructure.audit",
        "com.turnosodontologia.publicbooking",
        "com.turnosodontologia.depositpayments"})
@Import(AuthPasswordHasher.class)
@EnableJpaRepositories(basePackages = "com.turnosodontologia.infrastructure.persistence")
public class TestJpaConfig {

    @Bean
    @ConditionalOnMissingBean(CurrentActorProvider.class)
    public CurrentActorProvider currentActorProvider() {
        return new SimulatedCurrentActorProvider();
    }

    @Bean
    @ConditionalOnMissingBean(AuditEventPublisher.class)
    public AuditEventPublisher auditEventPublisher() {
        return new InMemoryAuditEventRecorder();
    }
}
