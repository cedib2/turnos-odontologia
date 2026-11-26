package com.turnosodontologia.application.notificaciones;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/** Elige la implementación de {@link AvisosTurno} según {@code notificaciones.turnos.whatsapp}. */
@Configuration
public class AvisosTurnoConfig {

    @Bean
    @ConditionalOnProperty(name = "notificaciones.turnos.whatsapp", havingValue = "true")
    public AvisosTurno avisosTurnoWhatsApp(
            NotificacionService notificaciones, NotificacionStore store, Clock reloj,
            @Value("${notificaciones.recordatorio.anticipacion-horas:#{null}}") Long anticipacionHoras) {
        if (anticipacionHoras != null && anticipacionHoras < 1) {
            throw new IllegalArgumentException("notificaciones.recordatorio.anticipacion-horas debe ser positiva");
        }
        return new AvisosTurnoWhatsApp(notificaciones, store, reloj,
                anticipacionHoras == null ? null : Duration.ofHours(anticipacionHoras));
    }

    @Bean
    @ConditionalOnProperty(name = "notificaciones.turnos.whatsapp", havingValue = "false", matchIfMissing = true)
    public AvisosTurno avisosTurnoDesactivados() {
        return AvisosTurno.NINGUNO;
    }
}
