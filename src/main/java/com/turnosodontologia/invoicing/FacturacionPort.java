package com.turnosodontologia.invoicing;

/** Frente de facturación de C-16 (AR-7), sin SDK ni dependencias nuevas. */
public interface FacturacionPort {

    FacturacionResultado emitir(SolicitudFacturacion solicitud);
}
