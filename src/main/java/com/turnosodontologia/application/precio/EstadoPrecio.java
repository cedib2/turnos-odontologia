package com.turnosodontologia.application.precio;

public enum EstadoPrecio {
    /** Monto y moneda configurados por el PO: el precio se publica. */
    PUBLICADO,
    /** Monto todavía (TBD): la superficie existe pero no muestra una cifra inventada. */
    PENDIENTE_DEFINICION
}
