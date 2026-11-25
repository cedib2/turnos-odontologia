package com.turnosodontologia.invoicing;

import java.util.Locale;

/** Solo acepta códigos controlados; cualquier otro texto se descarta (AR-9, AR-14). */
public final class ErrorSaneador {

    public static final String GENERICO = "error del proveedor (detalle omitido)";

    private ErrorSaneador() {
    }

    public static String sanear(String mensaje) {
        if (mensaje == null) {
            return GENERICO;
        }
        String codigo = mensaje.trim().toUpperCase(Locale.ROOT);
        return codigo.matches("[A-Z][A-Z0-9_]{2,39}") ? codigo : GENERICO;
    }
}
