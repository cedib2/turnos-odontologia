package com.turnosodontologia.publicbooking;

import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenScope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/publico/v1/turnos")
public class PublicTurnoController {

    private final PublicBookingService servicio;
    private final PublicTokenGuard guardia;

    public PublicTurnoController(PublicBookingService servicio, PublicTokenGuard guardia) {
        this.servicio = servicio;
        this.guardia = guardia;
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse disponibilidad(@RequestParam String token,
                                                 @RequestParam Long profesionalId,
                                                 @RequestParam(required = false) Long sillonId,
                                                 @RequestParam String fecha,
                                                 @RequestParam String codigoTratamiento) {
        guardia.validar(token, PublicTokenScope.RESERVAR);
        LocalDate dia;
        try {
            dia = LocalDate.parse(fecha);
        } catch (DateTimeParseException ex) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "fecha_invalida");
        }
        return servicio.disponibilidad(profesionalId, sillonId, dia, codigoTratamiento);
    }

    @PostMapping
    public CrearTurnoPublicoResponse crear(@RequestParam String token,
                                           @RequestBody CrearPublicoRequest solicitud) {
        PublicTokenClaims claims = guardia.validar(token, PublicTokenScope.RESERVAR).claims();
        return servicio.crear(claims, solicitud);
    }

    @PostMapping("/confirmar")
    public GestionarTurnoResponse confirmar(@RequestParam String token,
                                            @RequestBody GestionarRequest solicitud) {
        PublicTokenClaims claims = guardia.validar(token, PublicTokenScope.GESTIONAR,
                solicitud.turnoId()).claims();
        return servicio.confirmar(claims, solicitud);
    }

    @PostMapping("/cancelar")
    public GestionarTurnoResponse cancelar(@RequestParam String token,
                                           @RequestBody GestionarRequest solicitud) {
        PublicTokenClaims claims = guardia.validar(token, PublicTokenScope.GESTIONAR,
                solicitud.turnoId()).claims();
        return servicio.cancelar(claims, solicitud);
    }

    @PostMapping("/reprogramar")
    public GestionarTurnoResponse reprogramar(@RequestParam String token,
                                              @RequestBody ReprogramarRequest solicitud) {
        PublicTokenClaims claims = guardia.validar(token, PublicTokenScope.GESTIONAR,
                solicitud.turnoId()).claims();
        return servicio.reprogramar(claims, solicitud);
    }

    @ExceptionHandler(PublicApiException.class)
    public ResponseEntity<PublicErrorResponse> errorPublico(PublicApiException excepcion) {
        return ResponseEntity.status(excepcion.status()).body(PublicErrorResponse.de(excepcion));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PublicErrorResponse> errorSolicitud(IllegalArgumentException excepcion) {
        return ResponseEntity.badRequest().body(PublicErrorResponse.simple(excepcion.getMessage()));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, CannotAcquireLockException.class})
    public ResponseEntity<PublicErrorResponse> errorConflicto(RuntimeException excepcion) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PublicErrorResponse.simple("hueco_tomado"));
    }
}
