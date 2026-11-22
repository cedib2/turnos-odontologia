package com.turnosodontologia.audit;

import com.turnosodontologia.application.BloqueoService;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.infrastructure.persistence.BloqueoRepository;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(RealAuditPublisherConfig.class)
class AuditoriaTransaccionTest extends PostgresFlywayTest {

    private static final Instant DESDE = Instant.parse("2026-12-20T13:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-12-20T14:00:00Z");

    @Autowired
    private BloqueoService bloqueoService;

    @Autowired
    private BloqueoRepository bloqueos;

    @Autowired
    private SillonRepository sillones;

    @Autowired
    private ConsultorioRepository consultorios;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long sillonId;

    @BeforeEach
    void preparar() {
        sillonId = sillones.saveAndFlush(new Sillon("S-AUDIT-TX",
                consultorios.findById(1L).orElseThrow())).getId();
        jdbcTemplate.execute("""
                CREATE FUNCTION rechaza_auditoria_prueba() RETURNS trigger AS $$
                BEGIN
                    IF NEW.motivo = 'FALLO_AUDITORIA' THEN
                        RAISE EXCEPTION 'fallo forzado de auditoria';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_auditoria_prueba
                    BEFORE INSERT ON auditoria_log
                    FOR EACH ROW EXECUTE FUNCTION rechaza_auditoria_prueba()
                """);
    }

    @Test
    void insertAuditoriaFallaYRevierteLaOperacionDeNegocio() {
        assertThrows(Exception.class, () -> bloqueoService.crearBloqueo(
                null, sillonId, DESDE, HASTA, "FALLO_AUDITORIA"));

        assertEquals(0, bloqueos.count());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log", Long.class));
    }

    @Test
    void rollbackDeNegocioTrasPublicarEventoNoDejaFilaDeAuditoria() {
        assertThrows(IllegalStateException.class, () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
                    bloqueoService.crearBloqueo(null, sillonId,
                            DESDE.plusSeconds(7200), HASTA.plusSeconds(7200), "rollback-motivo");
                    throw new IllegalStateException("rollback de negocio");
                }));

        assertEquals(0, bloqueos.count());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_log", Long.class));
    }
}
