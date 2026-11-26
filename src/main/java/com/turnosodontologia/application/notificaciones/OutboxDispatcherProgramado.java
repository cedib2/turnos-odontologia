package com.turnosodontologia.application.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;

/**
 * Despacho periódico del outbox de WhatsApp. Solo se activa con
 * {@code notificaciones.dispatcher.habilitado=true} (encendido en dev; en test y fuera de dev queda apagado
 * hasta que exista el adaptador real de WhatsApp Business API). Cada pasada toma un lote con
 * {@code FOR UPDATE SKIP LOCKED}, así que varias instancias no envían dos veces el mismo mensaje.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "notificaciones.dispatcher.habilitado", havingValue = "true")
public class OutboxDispatcherProgramado {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxDispatcherProgramado.class);

    private final OutboxDispatcher dispatcher;
    private final Clock reloj;
    private final int lote;

    public OutboxDispatcherProgramado(OutboxDispatcher dispatcher, Clock reloj,
                                      @Value("${notificaciones.dispatcher.lote:50}") int lote) {
        if (lote < 1) {
            throw new IllegalArgumentException("notificaciones.dispatcher.lote debe ser positivo");
        }
        this.dispatcher = dispatcher;
        this.reloj = reloj;
        this.lote = lote;
    }

    @Scheduled(fixedDelayString = "${notificaciones.dispatcher.intervalo-ms:60000}",
            initialDelayString = "${notificaciones.dispatcher.demora-inicial-ms:10000}")
    public void despachar() {
        try {
            int procesadas = dispatcher.procesar(reloj.instant(), lote);
            if (procesadas > 0) {
                LOG.info("Outbox de WhatsApp: {} notificaciones procesadas", procesadas);
            }
        } catch (RuntimeException e) {
            // El lote se revierte completo (transacción del dispatcher) y se reintenta en la próxima pasada.
            LOG.error("Falló el despacho del outbox de WhatsApp: {}", e.getMessage(), e);
        }
    }
}
