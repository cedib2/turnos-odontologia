package com.turnosodontologia.application.clinico;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turnosodontologia.application.cumplimiento.ProveedorHistoriaClinica;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Implementa el contrato de C-17 con C-11/C-13: la copia por paciente incluye la historia clínica vigente,
 * la misma vista que C-13 le entrega al titular. La autorización la da la operación de exportación (C-17),
 * no un rol clínico. Las fechas salen como texto ISO-8601 para que el paquete no dependa de módulos de Jackson.
 */
@Component
public class HistoriaClinicaParaExportacion implements ProveedorHistoriaClinica {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final HistoriaClinicaService historias;

    public HistoriaClinicaParaExportacion(HistoriaClinicaService historias) {
        this.historias = historias;
    }

    @Override
    public Optional<Map<String, Object>> copiaHistoria(long pacienteId) {
        return historias.historiaDelTitular(pacienteId)
                .map(historia -> MAPPER.convertValue(historia, new TypeReference<Map<String, Object>>() {
                }));
    }
}
