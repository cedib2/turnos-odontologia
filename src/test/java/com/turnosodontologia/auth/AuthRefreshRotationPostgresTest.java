package com.turnosodontologia.auth;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.RolService;
import com.turnosodontologia.application.SimulatedCurrentActorProvider;
import com.turnosodontologia.application.UsuarioService;
import com.turnosodontologia.domain.identidad.ProveedorAutenticacion;
import com.turnosodontologia.domain.identidad.Rol;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AuthRefreshRotationPostgresTest.Config.class,
        properties = "AUTH_JWT_SECRET=clave-de-prueba-no-real-para-tests")
@ActiveProfiles("test")
class AuthRefreshRotationPostgresTest {

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", com.turnosodontologia.support.PostgresFixture::jdbcUrl);
        registry.add("spring.datasource.username", com.turnosodontologia.support.PostgresFixture::user);
        registry.add("spring.datasource.password", com.turnosodontologia.support.PostgresFixture::password);
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthService authService;

    @Autowired
    private RolService rolService;

    @Autowired
    private UsuarioService usuarioService;

    @BeforeEach
    void rebuildSchema() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void dosRotacionesConcurrentesSoloUnaSeConfirma() throws Exception {
        String refresco = crearRefresh("concurrencia-" + UUID.randomUUID() + "@example.com");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        java.util.concurrent.Callable<AuthService.RotationResult> tarea = () -> {
            salida.await();
            return authService.rotar(refresco);
        };
        List<Future<AuthService.RotationResult>> futuros =
                List.of(executor.submit(tarea), executor.submit(tarea));
        salida.countDown();

        int aceptadas = 0;
        int rechazadas = 0;
        try {
            for (Future<AuthService.RotationResult> futuro : futuros) {
                AuthService.RotationResult resultado = futuro.get(30, TimeUnit.SECONDS);
                if (resultado.esValido()) {
                    aceptadas++;
                } else {
                    rechazadas++;
                }
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, aceptadas);
        assertEquals(1, rechazadas);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE used_at IS NOT NULL", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE revoked_at IS NOT NULL", Integer.class));
    }

    @Test
    void logoutRevocaSesionFamiliaYJtiEnPostgres() {
        String email = "logout-" + UUID.randomUUID() + "@example.com";
        Rol rol = rolService.registrar("log-" + UUID.randomUUID().toString().substring(0, 8), "Recepcionista");
        usuarioService.registrar(rol, email, "ClaveSegura123!", ProveedorAutenticacion.LOCAL);
        AuthService.LoginResult resultado = authService.login(
                email, "ClaveSegura123!", "127.0.0.1", "test");

        authService.logout(resultado.jti());

        assertEquals("REVOCADA", jdbcTemplate.queryForObject(
                "SELECT estado FROM auth_sesiones WHERE jti = ?", String.class, resultado.jti()));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM auth_sesiones WHERE jti = ?", java.sql.Timestamp.class, resultado.jti()));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_tokens_refresh WHERE revoked_at IS NULL", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_jti_revocados WHERE jti = ?", Integer.class, resultado.jti()));
    }

    private String crearRefresh(String email) {
        Rol rol = rolService.registrar("conc-" + UUID.randomUUID().toString().substring(0, 8), "Recepcionista");
        usuarioService.registrar(rol, email, "ClaveSegura123!", ProveedorAutenticacion.LOCAL);
        return authService.login(email, "ClaveSegura123!", "127.0.0.1", "test").refresh();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = {"com.turnosodontologia.domain", "com.turnosodontologia.auth"})
    @ComponentScan(basePackages = {"com.turnosodontologia.application",
            "com.turnosodontologia.infrastructure.audit"})
    @Import({AuthService.class, AuthRevocationService.class, AuthPasswordHasher.class, JwtService.class})
    @EnableJpaRepositories(basePackages = {"com.turnosodontologia.infrastructure.persistence",
            "com.turnosodontologia.auth"})
    static class Config {

        @Bean
        @ConditionalOnMissingBean(CurrentActorProvider.class)
        CurrentActorProvider currentActorProvider() {
            return new SimulatedCurrentActorProvider();
        }

        @Bean
        @ConditionalOnMissingBean(AuditEventPublisher.class)
        AuditEventPublisher auditEventPublisher() {
            return new InMemoryAuditEventRecorder();
        }
    }
}
