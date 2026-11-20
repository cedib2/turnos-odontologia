package com.turnosodontologia.application;

import com.turnosodontologia.domain.auditoria.ActorSimulado;

public interface CurrentActorProvider {

    ActorSimulado current();

    ActorSimulado system();
}
