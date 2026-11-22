package com.turnosodontologia.auth;

public record AuthenticatedActor(Long usuarioId, String nombre, String rol, String jti, ModoActor modo) {

    public static AuthenticatedActor real(Long usuarioId, String nombre, String rol, String jti) {
        return new AuthenticatedActor(usuarioId, nombre, rol, jti, ModoActor.REAL);
    }

    public static AuthenticatedActor simulado(String nombre, String rol, String jti) {
        return new AuthenticatedActor(null, nombre, rol, jti, ModoActor.SIMULADO);
    }

    public static AuthenticatedActor sistema() {
        return new AuthenticatedActor(null, "system", null, null, ModoActor.SYSTEM);
    }

    public static AuthenticatedActor paciente(Long usuarioId, String nombre) {
        return new AuthenticatedActor(usuarioId, nombre, "paciente", null, ModoActor.PACIENTE);
    }
}
