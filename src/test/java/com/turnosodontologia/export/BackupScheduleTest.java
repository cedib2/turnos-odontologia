package com.turnosodontologia.export;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupScheduleTest {

    private static final String CRON = "0 0 3 * * *";
    private static final int RETENCION_ANIOS = 10;
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    void respaldoDiario0300() {
        ExportOperationalConfig config = config();

        assertAll(
                () -> assertEquals(CRON, config.cron()),
                () -> assertEquals(RETENCION_ANIOS, config.retencionAnios()),
                () -> assertEquals(ZONA, config.zona()),
                () -> assertEquals(Instant.parse("2026-11-25T06:00:00Z"),
                        config.proximaEjecucion(Instant.parse("2026-11-25T02:00:00Z")).toInstant()),
                () -> assertEquals(Instant.parse("2026-11-26T06:00:00Z"),
                        config.proximaEjecucion(Instant.parse("2026-11-25T06:00:00Z")).toInstant())
        );
    }

    @Test
    void retencionDiezAniosSinBorradoFisico() {
        LocalBackupStorage storage = new LocalBackupStorage(clave());
        storage.guardar("tenant-1", "antiguo", "contenido".getBytes(StandardCharsets.UTF_8),
                Instant.parse("2016-01-01T06:00:00Z"));

        new BackupScheduleService(config(), storage, new InMemoryAuditEventRecorder(),
                new InMemoryOpsAlertPort()).aplicarRetencion(storage, Instant.parse("2026-11-25T06:00:00Z"));

        assertAll(
                () -> assertTrue(storage.listar("tenant-1").contains("antiguo")),
                () -> assertTrue(storage.objeto("tenant-1", "antiguo").expiradoLogico()),
                () -> assertEquals("contenido", new String(
                        storage.leer("tenant-1", "antiguo"), StandardCharsets.UTF_8))
        );
    }

    @Test
    void falloConBackupFailedYAlerta() {
        BackupStoragePort storage = new BackupStoragePort() {
            @Override
            public void guardar(String tenantId, String artefactoId, byte[] contenido) {
                throw new IllegalStateException("destino indisponible");
            }

            @Override
            public byte[] leer(String tenantId, String artefactoId) {
                return null;
            }

            @Override
            public List<String> listar(String tenantId) {
                return List.of();
            }
        };
        InMemoryAuditEventRecorder eventos = new InMemoryAuditEventRecorder();
        InMemoryOpsAlertPort alertas = new InMemoryOpsAlertPort();
        BackupScheduleService servicio = new BackupScheduleService(config(), storage, eventos, alertas);

        assertThrows(IllegalStateException.class, () -> servicio.ejecutar(
                "tenant-1", "respaldo-1", "zip".getBytes(StandardCharsets.UTF_8),
                "corr-bk-ko", ActorSimulado.system()));

        assertAll(
                () -> assertEquals(List.of("BACKUP_FAILED"),
                        eventos.eventos().stream().map(AuditEvent::tipo).toList()),
                () -> assertEquals("destino indisponible",
                        eventos.eventos().get(0).metadata().get("motivo")),
                () -> assertEquals(1, alertas.alertas().size()),
                () -> assertTrue(alertas.alertas().get(0).tipo().contains("BACKUP_FAILED"))
        );
    }

    private ExportOperationalConfig config() {
        return new ExportOperationalConfig(new MockEnvironment()
                .withProperty(ExportOperationalConfig.CRON_PROPERTY, CRON)
                .withProperty(ExportOperationalConfig.RETENCION_PROPERTY, String.valueOf(RETENCION_ANIOS))
                .withProperty(ExportOperationalConfig.CLAVE_PROPERTY,
                        Base64.getEncoder().encodeToString(clave())));
    }

    private byte[] clave() {
        try {
            KeyGenerator generador = KeyGenerator.getInstance("AES");
            generador.init(256);
            return generador.generateKey().getEncoded();
        } catch (Exception excepcion) {
            throw new IllegalStateException(excepcion);
        }
    }
}
