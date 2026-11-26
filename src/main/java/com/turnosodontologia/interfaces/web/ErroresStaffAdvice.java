package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.AccesoFueraDeAlcanceException;
import com.turnosodontologia.application.clinico.AccesoClinicoDenegadoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Errores de la superficie de staff con un status y un código legibles, en lugar de un 500 genérico:
 * <ul>
 *   <li>409 {@code solapamiento} o {@code conflicto_de_datos} ante violaciones de integridad (RN-AG-01);</li>
 *   <li>403 {@code sin_permiso} o el motivo del ámbito {@code propio} (RN-SE-01);</li>
 *   <li>400 con el mensaje de validación y 409 ante un estado que no admite la operación.</li>
 * </ul>
 */
@RestControllerAdvice(basePackageClasses = ErroresStaffAdvice.class)
public class ErroresStaffAdvice {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflicto(DataIntegrityViolationException excepcion) {
        String detalle = String.valueOf(excepcion.getMostSpecificCause().getMessage());
        String codigo = detalle.contains("exclusion constraint") ? "solapamiento" : "conflicto_de_datos";
        return respuesta(HttpStatus.CONFLICT, codigo);
    }

    @ExceptionHandler(AccesoClinicoDenegadoException.class)
    public ResponseEntity<Map<String, String>> sinPermiso(AccesoClinicoDenegadoException excepcion) {
        return respuesta(HttpStatus.FORBIDDEN, "sin_permiso");
    }

    @ExceptionHandler(AccesoFueraDeAlcanceException.class)
    public ResponseEntity<Map<String, String>> fueraDeAlcance(AccesoFueraDeAlcanceException excepcion) {
        return respuesta(HttpStatus.FORBIDDEN, excepcion.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalido(IllegalArgumentException excepcion) {
        return respuesta(HttpStatus.BAD_REQUEST, String.valueOf(excepcion.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> estado(IllegalStateException excepcion) {
        return respuesta(HttpStatus.CONFLICT, String.valueOf(excepcion.getMessage()));
    }

    private static ResponseEntity<Map<String, String>> respuesta(HttpStatus status, String codigo) {
        return ResponseEntity.status(status).body(Map.of("error", codigo));
    }
}
