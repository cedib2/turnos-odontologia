package com.turnosodontologia.publicbooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turnosodontologia.publictoken.PublicResponseHeaders;
import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicTurnoControllerTest {

    private static final ObjectMapper MAPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private PublicBookingService servicio;

    @Mock
    private PublicTokenGuard guardia;

    private MockMvc mvc;

    @BeforeEach
    void construir() {
        PublicTurnoController controlador = new PublicTurnoController(servicio, guardia);
        mvc = MockMvcBuilders.standaloneSetup(controlador)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(MAPER))
                .setControllerAdvice(new PublicResponseHeaders(List.of("/publico/v1")))
                .build();
    }

    @Test
    void disponibilidadSinCredencialesDeStaff() throws Exception {
        when(guardia.validar("token-reservar", PublicTokenScope.RESERVAR))
                .thenReturn(new PublicTokenGuard.Validacion(claimsReservar()));
        when(servicio.disponibilidad(eq(7L), isNull(), any(), eq("LIMPIEZA")))
                .thenReturn(new DisponibilidadResponse(7L, null, "2026-12-03", "LIMPIEZA",
                        List.of(new HuecoResponse(Instant.parse("2026-12-03T13:00:00Z"),
                                Instant.parse("2026-12-03T13:30:00Z")))));

        mvc.perform(get("/publico/v1/turnos/disponibilidad")
                        .param("token", "token-reservar")
                        .param("profesionalId", "7")
                        .param("fecha", "2026-12-03")
                        .param("codigoTratamiento", "LIMPIEZA"))
                .andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.huecos[0].inicio").value("2026-12-03T13:00:00Z"));

        verify(guardia).validar("token-reservar", PublicTokenScope.RESERVAR);
    }

    @Test
    void crearDevuelveTokenDeGestion() throws Exception {
        when(guardia.validar("token-reservar", PublicTokenScope.RESERVAR))
                .thenReturn(new PublicTokenGuard.Validacion(claimsReservar()));
        when(servicio.crear(any(), any())).thenReturn(new CrearTurnoPublicoResponse(11L,
                Instant.parse("2026-12-03T13:00:00Z"), Instant.parse("2026-12-03T13:30:00Z"),
                "reservado", "link", "token-gestionar"));

        mvc.perform(post("/publico/v1/turnos")
                        .param("token", "token-reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pacienteId":42,"profesionalId":7,"sillonId":3,
                                 "codigoTratamiento":"LIMPIEZA","inicio":"2026-12-03T13:00:00Z",
                                 "idempotency_key":"clave-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(jsonPath("$.estado").value("reservado"))
                .andExpect(jsonPath("$.origen").value("link"))
                .andExpect(jsonPath("$.tokenGestionar").value("token-gestionar"));
    }

    @Test
    void gestionarValidaScopeYTurnoDelClaim() throws Exception {
        when(guardia.validar("token-gestionar", PublicTokenScope.GESTIONAR, 11L))
                .thenReturn(new PublicTokenGuard.Validacion(claimsGestionar(11L)));
        when(servicio.confirmar(any(), any())).thenReturn(new GestionarTurnoResponse(11L,
                "confirmado", true, Instant.parse("2026-12-03T13:00:00Z"),
                Instant.parse("2026-12-03T13:30:00Z")));

        mvc.perform(post("/publico/v1/turnos/confirmar")
                        .param("token", "token-gestionar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"turnoId\":11,\"idempotency_key\":\"clave-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("confirmado"));

        verify(guardia).validar("token-gestionar", PublicTokenScope.GESTIONAR, 11L);
    }

    @Test
    void scopeIncorrectoSeRechazaSinEfectos() throws Exception {
        when(guardia.validar("token-reservar", PublicTokenScope.GESTIONAR, 11L))
                .thenThrow(new PublicApiException(HttpStatus.BAD_REQUEST, "scope_no_permitido"));

        mvc.perform(post("/publico/v1/turnos/cancelar")
                        .param("token", "token-reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"turnoId\":11,\"idempotency_key\":\"clave-3\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"scope_no_permitido\"}"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void tokenVencidoPideNuevoEnlace() throws Exception {
        when(guardia.validar("token-vencido", PublicTokenScope.RESERVAR))
                .thenThrow(PublicApiException.vencido("token_vencido"));

        mvc.perform(get("/publico/v1/turnos/disponibilidad")
                        .param("token", "token-vencido")
                        .param("profesionalId", "7")
                        .param("fecha", "2026-12-03")
                        .param("codigoTratamiento", "LIMPIEZA"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("token_vencido"))
                .andExpect(jsonPath("$.requiereNuevoEnlace").value(true));
    }

    @Test
    void reenvioDuplicadoDevuelveConflicto() throws Exception {
        when(guardia.validar("token-reservar", PublicTokenScope.RESERVAR))
                .thenReturn(new PublicTokenGuard.Validacion(claimsReservar()));
        when(servicio.crear(any(), any()))
                .thenThrow(new PublicApiException(HttpStatus.CONFLICT, "operacion_duplicada"));

        mvc.perform(post("/publico/v1/turnos")
                        .param("token", "token-reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pacienteId\":42,\"profesionalId\":7,\"sillonId\":3,"
                                + "\"codigoTratamiento\":\"LIMPIEZA\","
                                + "\"inicio\":\"2026-12-03T13:00:00Z\","
                                + "\"idempotency_key\":\"clave-1\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"error\":\"operacion_duplicada\"}"));
    }

    @Test
    void fueraDeVentanaDerivaARecepcion() throws Exception {
        when(guardia.validar("token-gestionar", PublicTokenScope.GESTIONAR, 11L))
                .thenReturn(new PublicTokenGuard.Validacion(claimsGestionar(11L)));
        when(servicio.cancelar(any(), any())).thenThrow(PublicApiException.fueraDeVentana());

        mvc.perform(post("/publico/v1/turnos/cancelar")
                        .param("token", "token-gestionar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"turnoId\":11,\"idempotency_key\":\"clave-4\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.derivarARecepcion").value(true));
    }

    private static PublicTokenClaims claimsReservar() {
        long ahora = Instant.parse("2026-12-01T12:00:00Z").getEpochSecond();
        return new PublicTokenClaims("jti-reservar", "turnos-odontologia-public", "paciente-42",
                "42", PublicTokenScope.RESERVAR, ahora, ahora + 3600, null, false);
    }

    private static PublicTokenClaims claimsGestionar(Long turnoId) {
        long ahora = Instant.parse("2026-12-01T12:00:00Z").getEpochSecond();
        return new PublicTokenClaims("jti-gestionar", "turnos-odontologia-public", "paciente-42",
                "42", PublicTokenScope.GESTIONAR, ahora, ahora + 604800,
                String.valueOf(turnoId), false);
    }

    @Test
    void superficieSoloExponeElPrefijoPublico() {
        org.springframework.web.bind.annotation.RequestMapping mapping =
                PublicTurnoController.class.getAnnotation(
                        org.springframework.web.bind.annotation.RequestMapping.class);
        org.junit.jupiter.api.Assertions.assertNotNull(mapping);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"/publico/v1/turnos"}, mapping.value());
    }
}
