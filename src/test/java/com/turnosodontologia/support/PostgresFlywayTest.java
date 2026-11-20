package com.turnosodontologia.support;

import com.turnosodontologia.TestJpaConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = TestJpaConfig.class)
@ActiveProfiles("test")
public abstract class PostgresFlywayTest {

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresFixture::jdbcUrl);
        registry.add("spring.datasource.username", PostgresFixture::user);
        registry.add("spring.datasource.password", PostgresFixture::password);
    }

    @Autowired
    protected Flyway flyway;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void rebuildSchema() {
        flyway.clean();
        flyway.migrate();
    }
}
