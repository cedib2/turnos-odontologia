package com.turnosodontologia.waitlist;

import java.util.HashMap;
import java.util.Map;

/**
 * Metadata cerrada de los seis eventos de C-19 (W-10). Solo identificadores,
 * período, cifras, metodología, motivo y correlationId: nunca datos personales
 * ni credenciales (AuditoriaMetadataValidator).
 */
public final class WaitlistAuditMetadata {

    private final String correlationId;
    private final Map<String, Object> valores = new HashMap<>();

    private WaitlistAuditMetadata(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("El correlationId es obligatorio");
        }
        this.correlationId = correlationId;
        valores.put("correlationId", correlationId);
    }

    public static WaitlistAuditMetadata de(String correlationId) {
        return new WaitlistAuditMetadata(correlationId);
    }

    public String correlationId() {
        return correlationId;
    }

    public WaitlistAuditMetadata entrada(Long id) {
        return poner("entradaId", id);
    }

    public WaitlistAuditMetadata turno(Long id) {
        return poner("turnoId", id);
    }

    public WaitlistAuditMetadata senia(Long id) {
        return poner("seniaId", id);
    }

    public WaitlistAuditMetadata metrica(Long id) {
        return poner("metricaId", id);
    }

    public WaitlistAuditMetadata sillon(Long id) {
        return poner("sillonId", id);
    }

    public WaitlistAuditMetadata periodo(String periodo) {
        return poner("periodo", periodo);
    }

    public WaitlistAuditMetadata ocupacion(Object valor) {
        return poner("ocupacion", valor);
    }

    public WaitlistAuditMetadata ausentismo(Object valor) {
        return poner("ausentismo", valor);
    }

    public WaitlistAuditMetadata metodologia(String valor) {
        return poner("metodologia", valor);
    }

    public WaitlistAuditMetadata motivo(String motivo) {
        return poner("motivo", motivo);
    }

    private WaitlistAuditMetadata poner(String clave, Object valor) {
        if (valor != null) {
            valores.put(clave, valor);
        }
        return this;
    }

    public Map<String, Object> build() {
        return Map.copyOf(valores);
    }
}
