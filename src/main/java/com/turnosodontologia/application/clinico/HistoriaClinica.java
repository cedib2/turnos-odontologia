package com.turnosodontologia.application.clinico;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Vista vigente de la historia: última versión de cada registro, sin perder el historial. */
public record HistoriaClinica(long fichaId, long pacienteId, int historiaVersion,
                              Anamnesis anamnesis, List<RegistroOdontograma> odontograma,
                              List<Evolucion> evoluciones, List<Consentimiento> consentimientos) {

    public record Anamnesis(long id, int version, Map<String, Object> respuestas, String textoLibre,
                            Instant fecha, String autor) {
    }

    public record RegistroOdontograma(long id, int piezaFdi, boolean temporal, String cara, String estado,
                                      int version, Instant fecha, String autor) {
    }

    public record Evolucion(long id, long profesionalId, int version, Long reemplazaId, String texto,
                            Instant fecha, String autor) {
    }

    public record Consentimiento(long id, String tipo, int version, String texto, Instant fecha,
                                 String autor) {
    }
}
