package com.turnosodontologia.waitlist;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Publica los seis eventos de C-19 con la hora del {@link Clock} inyectado,
 * nunca de {@code Instant.now()} (W-6). La metadata es la cerrada de W-10.
 */
@Component
public class WaitlistAuditPublisher {

    private final AuditEventPublisher auditoria;
    private final Clock reloj;

    public WaitlistAuditPublisher(AuditEventPublisher auditoria, Clock reloj) {
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public void publicar(String tipo, String entidad, Long entidadId, ActorSimulado actor,
                         String motivo, WaitlistAuditMetadata metadata) {
        auditoria.publish(new AuditEvent(tipo, entidad, entidadId, actor, reloj.instant(),
                motivo, metadata.build(), metadata.correlationId()));
    }
}
