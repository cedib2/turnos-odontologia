package com.turnosodontologia.application.cumplimiento;

import java.time.Instant;

/** Fila de {@code cumplimiento_operaciones} (V030). */
public record OperacionCumplimiento(Long id, String tipo, String alcance, Long pacienteId, String estado,
                                    String formato, String destino, boolean cifrado, Long respaldoOrigenId,
                                    String entornoRestore, String motivo, String solicitadoPor,
                                    Instant solicitadoAt, Instant plazoVenceAt, Instant completadoAt,
                                    Instant retenerHasta, String checksumSha256, Integer tablas, Long filas,
                                    String error) {

    public static final String EXPORTACION = "EXPORTACION";
    public static final String RESPALDO = "RESPALDO";
    public static final String RESTORE = "RESTORE";
    public static final String COMPLETA = "COMPLETA";
    public static final String PACIENTE = "PACIENTE";
    public static final String SOLICITADA = "SOLICITADA";
    public static final String COMPLETADA = "COMPLETADA";
    public static final String FALLIDA = "FALLIDA";

    public boolean completada() {
        return COMPLETADA.equals(estado);
    }
}
