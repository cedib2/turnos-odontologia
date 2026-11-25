package com.turnosodontologia.cumplimiento;

import com.turnosodontologia.audit.AuditoriaCanonicalizer;
import com.turnosodontologia.audit.AuditoriaIntegridadService;
import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base común de los tests de la Parte 4 con auditoría persistente y entorno de C-17. Compartir la
 * configuración permite reutilizar un único contexto de Spring (y su pool de conexiones).
 */
@Import({RealAuditPublisherConfig.class, AuditoriaIntegridadService.class, AuditoriaCanonicalizer.class,
        CumplimientoFixtures.class})
public abstract class CumplimientoPostgresTest extends PostgresFlywayTest {

    @DynamicPropertySource
    static void cumplimiento(DynamicPropertyRegistry registry) {
        CumplimientoEntorno.registrar(registry);
    }
}
