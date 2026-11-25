package com.turnosodontologia.invoicing;

/** Aviso interno cuando falta un dato del receptor (AR-6); sin envío real. */
public interface AvisoRecepcionPort {

    void solicitar(Long seniaId, String motivo);
}
