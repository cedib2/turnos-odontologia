package com.turnosodontologia.application.cumplimiento;

/**
 * Destino de exportaciones y respaldos. El destino productivo (cloud región AR, B6) sigue (TBD);
 * este puerto permite reemplazarlo sin tocar la lógica de C-17.
 */
public interface AlmacenArtefactos {

    /** Guarda el artefacto (ya cifrado) y devuelve la referencia de destino que se registra. */
    String guardar(String clave, byte[] contenido);

    byte[] leer(String referencia);

    boolean configurado();
}
