package com.turnosodontologia.domain.auditoria;

public record ActorSimulado(Long usuarioId, String nombre, String rol, String jti, String modo) {

    public ActorSimulado(String nombre, String modo) {
        this(null, nombre, null, null, modo);
    }

    public static ActorSimulado recepcionista() {
        return new ActorSimulado(null, "recepcionista", "RECEPCIONISTA", null, "SIMULADO");
    }

    public static ActorSimulado system() {
        return new ActorSimulado(null, "system", "SYSTEM", null, "SYSTEM");
    }

    public static ActorSimulado real(Long usuarioId, String nombre, String rol, String jti) {
        return new ActorSimulado(usuarioId, nombre, rol, jti, "REAL");
    }

    public static ActorSimulado paciente(Long usuarioId, String nombre) {
        return new ActorSimulado(usuarioId, nombre, "PACIENTE", null, "PACIENTE");
    }

    public static ActorSimulado pacienteSinUsuario() {
        return new ActorSimulado(null, "paciente", "PACIENTE", null, "PACIENTE");
    }
}
