package com.turnosodontologia.application;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Actor fijo de dev/test (modo {@code SIMULADO}). Por defecto es el recepcionista; con {@code turnos.simulado.rol}
 * se puede simular otro rol de staff (p. ej. {@code odontologo}) para probar sus permisos sin login. El
 * profesional vinculado de ese actor se configura con {@code turnos.simulado.profesional-id}.
 */
@Component
@Profile({"dev", "test"})
public class SimulatedCurrentActorProvider implements CurrentActorProvider {

    @Value("${turnos.simulado.rol:recepcionista}")
    private String rol = "recepcionista";

    @Override
    public ActorSimulado current() {
        if (rol == null || rol.isBlank() || "recepcionista".equalsIgnoreCase(rol)) {
            return ActorSimulado.recepcionista();
        }
        String codigo = rol.trim().toLowerCase(Locale.ROOT);
        return new ActorSimulado(null, codigo, codigo.toUpperCase(Locale.ROOT), null, "SIMULADO");
    }

    @Override
    public ActorSimulado system() {
        return ActorSimulado.system();
    }
}
