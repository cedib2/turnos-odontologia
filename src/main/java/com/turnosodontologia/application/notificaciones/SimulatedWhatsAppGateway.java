package com.turnosodontologia.application.notificaciones;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Gateway simulado para dev/test: registra los envíos y permite forzar fallos. */
@Component
@Profile({"dev", "test"})
public class SimulatedWhatsAppGateway implements WhatsAppGateway {

    private final List<EnvioWhatsApp> enviados = new CopyOnWriteArrayList<>();
    private final AtomicInteger fallosPendientes = new AtomicInteger();

    @Override
    public ResultadoEnvio enviar(EnvioWhatsApp envio) {
        if (fallosPendientes.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            return ResultadoEnvio.error("fallo-simulado");
        }
        enviados.add(envio);
        return ResultadoEnvio.exito("wamid.sim-" + UUID.randomUUID());
    }

    public void fallarProximos(int cantidad) {
        fallosPendientes.set(cantidad);
    }

    public List<EnvioWhatsApp> enviados() {
        return List.copyOf(enviados);
    }

    public void limpiar() {
        enviados.clear();
        fallosPendientes.set(0);
    }
}
