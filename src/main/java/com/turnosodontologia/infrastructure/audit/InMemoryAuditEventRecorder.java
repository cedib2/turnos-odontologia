package com.turnosodontologia.infrastructure.audit;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Profile("test")
public class InMemoryAuditEventRecorder implements AuditEventPublisher {

    private final List<AuditEvent> eventos = new CopyOnWriteArrayList<>();

    @Override
    public void publish(AuditEvent event) {
        eventos.add(event);
    }

    public List<AuditEvent> eventos() {
        return List.copyOf(eventos);
    }

    public void clear() {
        eventos.clear();
    }
}
