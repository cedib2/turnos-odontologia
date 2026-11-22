package com.turnosodontologia.application.clinico;

/**
 * Puerto hacia el proveedor de firma electrónica de consentimientos (Ley 25.506).
 * Proveedor e implementación son {@code (TBD)}: en dev/test se usa
 * {@link SimulatedFirmaElectronicaGateway}; fuera de esos perfiles
 * {@link FirmaElectronicaNoConfiguradaGateway} responde «no disponible».
 */
public interface FirmaElectronicaGateway {

    ResultadoFirma firmar(SolicitudFirma solicitud);

    record SolicitudFirma(long consentimientoId, long pacienteId, String hashDocumento) {
    }

    record ResultadoFirma(boolean firmado, String proveedor, String referencia, String error) {

        public static ResultadoFirma firmado(String proveedor, String referencia) {
            return new ResultadoFirma(true, proveedor, referencia, null);
        }

        public static ResultadoFirma noDisponible(String error) {
            return new ResultadoFirma(false, null, null, error);
        }
    }
}
