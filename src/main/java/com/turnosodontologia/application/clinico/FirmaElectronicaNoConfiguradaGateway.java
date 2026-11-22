package com.turnosodontologia.application.clinico;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fuera de dev/test, mientras el proveedor de firma electrónica sea {@code (TBD)}, ningún
 * consentimiento se da por firmado: falla cerrado y sin llamadas externas.
 */
@Component
@Profile("!dev & !test")
public class FirmaElectronicaNoConfiguradaGateway implements FirmaElectronicaGateway {

    public static final String ERROR = "proveedor-firma-electronica-no-configurado";

    @Override
    public ResultadoFirma firmar(SolicitudFirma solicitud) {
        return ResultadoFirma.noDisponible(ERROR);
    }
}
