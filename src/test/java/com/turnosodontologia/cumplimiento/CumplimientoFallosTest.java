package com.turnosodontologia.cumplimiento;

import com.turnosodontologia.application.cumplimiento.AlertaRespaldo;
import com.turnosodontologia.application.cumplimiento.OperacionCumplimiento;
import com.turnosodontologia.application.cumplimiento.RegistroOperaciones;
import com.turnosodontologia.application.cumplimiento.RespaldoService;
import com.turnosodontologia.audit.RealAuditPublisherConfig;
import com.turnosodontologia.support.PostgresFixture;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C-17: fallo de respaldo (sin clave de cifrado → nunca se escribe en claro) con alerta, y restore rechazado
 * cuando el «entorno aislado» resulta ser la base productiva.
 */
@Import({RealAuditPublisherConfig.class, CumplimientoFallosTest.AlertaConfig.class})
class CumplimientoFallosTest extends PostgresFlywayTest {

    static final List<OperacionCumplimiento> ALERTAS = new ArrayList<>();

    @TestConfiguration
    static class AlertaConfig {
        @Bean
        AlertaRespaldo alertaRespaldo() {
            return ALERTAS::add;
        }
    }

    @DynamicPropertySource
    static void cumplimiento(DynamicPropertyRegistry registry) {
        registry.add("cumplimiento.almacen.directorio", () -> CumplimientoEntorno.DIRECTORIO.toString());
        registry.add("cumplimiento.restore.aislado.url", PostgresFixture::jdbcUrl);
        registry.add("cumplimiento.restore.aislado.usuario", PostgresFixture::user);
        registry.add("cumplimiento.restore.aislado.clave", PostgresFixture::password);
    }

    @Autowired
    private RespaldoService respaldos;

    @Autowired
    private RegistroOperaciones registro;

    @BeforeEach
    void limpiar() {
        ALERTAS.clear();
    }

    @Test
    void sinClaveDeCifradoElRespaldoFallaAlertaYQuedaAuditado() {
        OperacionCumplimiento respaldo = respaldos.respaldar("respaldo manual");

        assertEquals("FALLIDA", respaldo.estado());
        assertTrue(respaldo.error().contains("BACKUP_ENCRYPTION_KEY"), respaldo.error());
        assertNull(respaldo.destino(), "no se escribe ningún artefacto sin cifrar");
        assertNull(respaldo.retenerHasta(), "retención (TBD) sin configurar: no se inventa");
        assertEquals(List.of(respaldo.id()), ALERTAS.stream().map(OperacionCumplimiento::id).toList());
        assertEquals("respaldo.fallido", jdbcTemplate.queryForObject(
                "SELECT accion FROM auditoria_log WHERE entidad = 'cumplimiento_operacion'", String.class));
    }

    @Test
    void restoreSobreLaBaseProductivaSeRechazaSinTocarDatos() {
        long respaldoId = registro.solicitar("RESPALDO", "COMPLETA", null, "JSON-v1", "fixture", "fixture",
                Instant.now(), null, null, null);
        registro.completar(respaldoId, "dir:inexistente.bin", true, "0".repeat(64), 1, 1, Instant.now(), null);
        long pacientes = jdbcTemplate.queryForObject("SELECT count(*) FROM pacientes", Long.class);
        long roles = jdbcTemplate.queryForObject("SELECT count(*) FROM roles", Long.class);

        OperacionCumplimiento restore = respaldos.restaurarEnAislado(respaldoId, "prueba");

        assertEquals("FALLIDA", restore.estado());
        assertTrue(restore.error().contains("base productiva"), restore.error());
        assertEquals(pacientes, jdbcTemplate.queryForObject("SELECT count(*) FROM pacientes", Long.class));
        assertEquals(roles, jdbcTemplate.queryForObject("SELECT count(*) FROM roles", Long.class));
        assertEquals("restore.fallido", jdbcTemplate.queryForObject(
                "SELECT accion FROM auditoria_log WHERE entidad = 'cumplimiento_operacion'", String.class));
        assertEquals(1, ALERTAS.size());
    }
}
