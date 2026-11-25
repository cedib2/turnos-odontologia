package com.turnosodontologia.interfaces.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Traduce las violaciones de integridad de la superficie de staff a 409 en lugar de un 500 genérico.
 * Las restricciones de exclusión de la agenda ({@code ex_*}, RN-AG-01) se informan como
 * {@code solapamiento} para que el frontend pueda explicar el motivo.
 */
@RestControllerAdvice(basePackageClasses = ConflictoDeDatosAdvice.class)
public class ConflictoDeDatosAdvice {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflicto(DataIntegrityViolationException excepcion) {
        String detalle = String.valueOf(excepcion.getMostSpecificCause().getMessage());
        String codigo = detalle.contains("exclusion constraint") ? "solapamiento" : "conflicto_de_datos";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", codigo));
    }
}
