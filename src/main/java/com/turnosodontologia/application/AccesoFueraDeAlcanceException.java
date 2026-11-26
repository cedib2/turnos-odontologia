package com.turnosodontologia.application;

/** El actor tiene el permiso pero con ámbito {@code propio} y el recurso no le pertenece (RN-SE-01). */
public class AccesoFueraDeAlcanceException extends RuntimeException {

    public AccesoFueraDeAlcanceException(String mensaje) {
        super(mensaje);
    }
}
