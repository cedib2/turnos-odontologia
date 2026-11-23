package com.turnosodontologia.invoicing;

/** Resultado acotado del proveedor; el detalle del error nunca viaja al exterior. */
public record FacturacionResultado(boolean exitoso, String cae, String codigoError) {

    public static FacturacionResultado aprobado(String cae) {
        return new FacturacionResultado(true, cae, null);
    }

    public static FacturacionResultado rechazado(String codigoError) {
        return new FacturacionResultado(false, null, codigoError);
    }
}
