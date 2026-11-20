package com.turnosodontologia.application;

import com.turnosodontologia.domain.auditoria.AuditEvent;

public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
