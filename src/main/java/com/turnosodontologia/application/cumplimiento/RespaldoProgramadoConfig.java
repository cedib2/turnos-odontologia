package com.turnosodontologia.application.cumplimiento;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Respaldo programado. Solo se activa cuando {@code BACKUP_CRON} está definido: la frecuencia sigue (TBD)
 * y no se inventa un valor por defecto (RN-GL-03).
 */
@Configuration
@EnableScheduling
@ConditionalOnExpression("'${cumplimiento.respaldo.cron:${BACKUP_CRON:}}'.trim().length() > 0")
public class RespaldoProgramadoConfig {

    private final RespaldoService respaldos;

    public RespaldoProgramadoConfig(RespaldoService respaldos) {
        this.respaldos = respaldos;
    }

    @Scheduled(cron = "${cumplimiento.respaldo.cron:${BACKUP_CRON:}}",
            zone = "America/Argentina/Buenos_Aires")
    public void ejecutar() {
        respaldos.respaldarProgramado();
    }
}
