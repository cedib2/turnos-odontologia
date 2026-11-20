package com.turnosodontologia.application;

import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class SimulatedCurrentActorProvider implements CurrentActorProvider {

    @Override
    public ActorSimulado current() {
        return ActorSimulado.recepcionista();
    }

    @Override
    public ActorSimulado system() {
        return ActorSimulado.system();
    }
}
