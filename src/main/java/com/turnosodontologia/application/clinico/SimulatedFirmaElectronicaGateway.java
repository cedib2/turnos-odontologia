package com.turnosodontologia.application.clinico;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Proveedor de firma simulado para dev/test; puede marcarse como no disponible. */
@Component
@Profile({"dev", "test"})
public class SimulatedFirmaElectronicaGateway implements FirmaElectronicaGateway {

    private volatile boolean disponible = true;

    @Override
    public ResultadoFirma firmar(SolicitudFirma solicitud) {
        if (!disponible) {
            return ResultadoFirma.noDisponible("proveedor-simulado-no-disponible");
        }
        return ResultadoFirma.firmado("simulado", "sim-" + UUID.randomUUID());
    }

    public void disponible(boolean valor) {
        this.disponible = valor;
    }
}
