package com.turnosodontologia;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.turnosodontologia.domain")
@EnableJpaRepositories(basePackages = "com.turnosodontologia.infrastructure.persistence")
public class TestJpaConfig {
}
