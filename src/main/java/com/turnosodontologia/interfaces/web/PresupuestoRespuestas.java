package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.domain.financiero.PresupuestoNoModificableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

final class PresupuestoRespuestas {

    private PresupuestoRespuestas() {
    }

    static ResponseEntity<?> ejecutar(Supplier<?> operacion) {
        try {
            return ResponseEntity.ok(operacion.get());
        } catch (PresupuestoNoModificableException e) {
            return conError(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return conError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            return conError(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return conError(HttpStatus.CONFLICT, "Conflicto con las restricciones del presupuesto");
        }
    }

    private static ResponseEntity<?> conError(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}
