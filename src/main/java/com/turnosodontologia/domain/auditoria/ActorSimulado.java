package com.turnosodontologia.domain.auditoria;

public record ActorSimulado(String nombre, String modo) {

    public static ActorSimulado recepcionista() {
        return new ActorSimulado("recepcionista", "SIMULADO");
    }

    public static ActorSimulado system() {
        return new ActorSimulado("system", "SIMULADO");
    }
}
