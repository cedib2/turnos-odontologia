package com.turnosodontologia.historiapaciente;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;

/**
 * Verificador SOLO para dev: acepta como evidencia el número de documento registrado del paciente, para poder
 * probar el acceso del titular a su historia de punta a punta. No es un mecanismo de verificación real: el
 * mecanismo definitivo sigue {@code (TBD)} (RN-CL-04) y fuera de dev/test la verificación falla cerrada.
 */
public class VerificadorIdentidadDocumentoDev implements VerificadorIdentidadTitular {

    private final JdbcTemplate jdbc;

    public VerificadorIdentidadDocumentoDev(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean verificar(long pacienteId, String evidencia) {
        if (evidencia == null || evidencia.isBlank()) {
            return false;
        }
        List<String> documentos = jdbc.queryForList(
                "SELECT documento_numero FROM pacientes WHERE id = ? AND activo", String.class, pacienteId);
        String normalizada = normalizar(evidencia);
        return documentos.stream().anyMatch(documento -> normalizar(documento).equals(normalizada));
    }

    private static String normalizar(String valor) {
        return valor.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT);
    }
}
