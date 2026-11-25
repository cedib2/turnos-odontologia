package com.turnosodontologia.invoicing;

import com.turnosodontologia.domain.financiero.Comprobante;

import java.util.Locale;

/**
 * Adaptador NO-OP/simulado para dev/test (AR-8): CAE determinista, marcado
 * explícitamente como de prueba y sin ninguna llamada de red.
 */
public final class FacturacionSimulada implements FacturacionPort {

    public static final String PREFIJO_CAE = "CAE-TEST-";
    public static final String PREFIJO_NOTA = "NC-TEST-";

    @Override
    public FacturacionResultado emitir(SolicitudFacturacion solicitud) {
        String referencia = solicitud.referencia() == null ? "sin-referencia" : solicitud.referencia();
        String sufijo = Integer.toHexString(referencia.hashCode()).toUpperCase(Locale.ROOT);
        String prefijo = Comprobante.TIPO_NOTA_CREDITO.equals(solicitud.tipo())
                ? PREFIJO_NOTA : PREFIJO_CAE;
        return FacturacionResultado.aprobado(prefijo + sufijo);
    }

    public static boolean esCaeDePrueba(String cae) {
        return cae != null && (cae.startsWith(PREFIJO_CAE) || cae.startsWith(PREFIJO_NOTA));
    }
}
