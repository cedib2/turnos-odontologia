package com.turnosodontologia.publicbooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.audit.InMemoryAuditEventRecorder;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenScope;
import com.turnosodontologia.publictoken.PublicTokenService;
import com.turnosodontologia.publictoken.PublicTokenSigner;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicBookingIntegrationTest extends PostgresFlywayTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String EMISOR = "turnos-odontologia-public";
    private static final String TRATAMIENTO = "TRAT-PUBLIC-INT";
    private static final ObjectMapper MAPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private PublicBookingService servicio;

    @Autowired
    private PublicTurnoController controlador;

    @Autowired
    private PublicTokenService tokens;

    @Autowired
    private PublicTokenSigner firmador;

    @Autowired
    private NotificacionPublicaPort notificaciones;

    @Autowired
    private TurnoRepository turnos;

    @Autowired
    private OcupacionRepository ocupaciones;

    @Autowired
    private PacienteRepository pacientes;

    @Autowired
    private ProfesionalRepository profesionales;

    @Autowired
    private SillonRepository sillones;

    @Autowired
    private ConsultorioRepository consultorios;

    @Autowired
    private TratamientoRepository tratamientos;

    @Autowired
    private HorarioAtencionRepository horarios;

    @Autowired
    private InMemoryAuditEventRecorder recorder;

    private MockMvc mvc;
    private LocalDate fecha;
    private Instant inicio;
    private Instant fin;

    @BeforeEach
    void preparar() {
        for (DayOfWeek dia : DayOfWeek.values()) {
            horarios.saveAndFlush(new HorarioAtencion(null, dia.getValue(),
                    LocalTime.of(13, 0), LocalTime.of(15, 0)));
        }
        tratamientos.saveAndFlush(new com.turnosodontologia.domain.agenda.Tratamiento(
                TRATAMIENTO, "Fixture público", 30));
        fecha = proximoDiaHabil(LocalDate.now(ZONA).plusDays(1));
        inicio = fecha.atTime(13, 0).atZone(ZONA).toInstant();
        fin = inicio.plusSeconds(1800);
        recorder.clear();
        ((NotificacionSimulada) notificaciones).clear();
        mvc = MockMvcBuilders.standaloneSetup(controlador)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(MAPER))
                .setControllerAdvice(new com.turnosodontologia.publictoken.PublicResponseHeaders(
                        List.of("/publico/v1")))
                .build();
    }

    @Test
    void flujoUnoReservarYConfirmarConTokenDeGestion() throws Exception {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-1");
        String tokenReservar = emitir(PublicTokenScope.RESERVAR, pacienteId, null);

        MvcResult respuesta = mvc.perform(post("/publico/v1/turnos")
                        .param("token", tokenReservar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPER.writeValueAsString(new CrearPublicoRequest(
                                pacienteId, profesionalId, sillonId, TRATAMIENTO, inicio, "idem-crear-1"))))
                .andExpect(status().isOk())
                .andReturn();
        CrearTurnoPublicoResponse creado = MAPER.readValue(
                respuesta.getResponse().getContentAsString(), CrearTurnoPublicoResponse.class);

        assertEquals("reservado", creado.estado());
        assertEquals("link", creado.origen());
        assertNotNull(creado.tokenGestionar());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertTrue(turnos.findByTokenPublico(firmador.verificar(creado.tokenGestionar()).jti())
                .isPresent());
        assertTrue(recorder.eventos().stream().anyMatch(evento -> "turno.creado".equals(evento.tipo())
                && "PACIENTE".equals(evento.actor().modo())));

        mvc.perform(post("/publico/v1/turnos/confirmar")
                        .param("token", creado.tokenGestionar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"turnoId\":" + creado.turnoId()
                                + ",\"idempotency_key\":\"idem-confirmar-1\"}"))
                .andExpect(status().isOk());

        assertEquals("confirmado", turnos.findById(creado.turnoId()).orElseThrow().getEstado());
        assertEquals(inicio, turnos.findById(creado.turnoId()).orElseThrow().getInicio());
        assertEquals(fin, turnos.findById(creado.turnoId()).orElseThrow().getFin());
        assertTrue(recorder.eventos().stream().anyMatch(evento -> "turno.confirmado".equals(evento.tipo())));
        assertEquals(2, ((NotificacionSimulada) notificaciones).solicitudes().size());
        assertTrue(recorder.eventos().stream().noneMatch(evento -> evento.tipo().startsWith("notificacion")));
        assertTrue(recorder.eventos().stream()
                .filter(evento -> "turno.creado".equals(evento.tipo()))
                .allMatch(evento -> evento.correlationId() != null && !evento.correlationId().isBlank()));
    }

    @Test
    void consultasReutilizanReservarYCreacionLoConsume() {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-8");
        String tokenReservar = crearTokenReservar(pacienteId);

        assertFalse(servicio.disponibilidad(profesionalId, sillonId, fecha, TRATAMIENTO).huecos().isEmpty());
        assertFalse(servicio.disponibilidad(profesionalId, sillonId, fecha, TRATAMIENTO).huecos().isEmpty());
        assertTrue(tokens.validar(tokenReservar).permitido());

        crear(tokenReservar, new CrearPublicoRequest(pacienteId, profesionalId, sillonId,
                TRATAMIENTO, inicio, "idem-consume-token"));

        var validacion = tokens.validar(tokenReservar);
        assertFalse(validacion.permitido());
        assertEquals("jti_reutilizado", validacion.motivo());
    }

    @Test
    void cancelarLiberaElHuecoEnLaMismaTransaccion() {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-2");
        CrearTurnoPublicoResponse creado = crear(crearTokenReservar(pacienteId),
                new CrearPublicoRequest(pacienteId, profesionalId, sillonId, TRATAMIENTO,
                        inicio, "idem-crear-2"));

        GestionarTurnoResponse cancelado = servicio.cancelar(
                tokens.validar(creado.tokenGestionar()).claims(),
                new GestionarRequest(creado.turnoId(), "idem-cancelar-2"));

        assertEquals("cancelado", cancelado.estado());
        assertFalse(cancelado.activo());
        assertTrue(ocupaciones.findByTurnoIdAndActivoTrue(creado.turnoId()).isEmpty());
        assertTrue(turnos.findById(creado.turnoId()).orElseThrow().getFin() != null);
        assertTrue(servicio.disponibilidad(profesionalId, null, fecha, TRATAMIENTO).huecos()
                .stream().anyMatch(hueco -> inicio.equals(hueco.inicio())));
        assertTrue(recorder.eventos().stream().anyMatch(evento -> "turno.cancelado".equals(evento.tipo())
                && inicio.toString().equals(evento.metadata().get("inicioAnterior"))),
                "turno.cancelado debe informar inicioAnterior (contrato con C-22)");
    }

    @Test
    void fueraDeVentanaDerivaARecepcionSinLiberar() {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-3");
        Instant lejano = fecha.plusDays(10).atTime(13, 0).atZone(ZONA).toInstant();
        CrearTurnoPublicoResponse creado = crear(crearTokenReservar(pacienteId),
                new CrearPublicoRequest(pacienteId, profesionalId, sillonId, TRATAMIENTO,
                        lejano, "idem-crear-3"));

        PublicApiException excepcion = org.junit.jupiter.api.Assertions.assertThrows(
                PublicApiException.class,
                () -> servicio.cancelar(tokens.validar(creado.tokenGestionar()).claims(),
                        new GestionarRequest(creado.turnoId(), "idem-cancelar-3")));

        assertTrue(excepcion.derivarARecepcion());
        assertEquals("reservado", turnos.findById(creado.turnoId()).orElseThrow().getEstado());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertTrue(recorder.eventos().stream().noneMatch(evento -> "turno.cancelado".equals(evento.tipo())));
    }

    @Test
    void reenvioConMismaClaveNoGeneraSegundoTurno() {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-4");
        CrearPublicoRequest solicitud = new CrearPublicoRequest(pacienteId, profesionalId, sillonId,
                TRATAMIENTO, inicio, "idem-duplicada");

        crear(crearTokenReservar(pacienteId), solicitud);
        PublicApiException excepcion = org.junit.jupiter.api.Assertions.assertThrows(
                PublicApiException.class,
                () -> crear(crearTokenReservar(pacienteId), solicitud));

        assertEquals("operacion_duplicada", excepcion.motivo());
        assertEquals(1, turnos.count());
    }

    @Test
    void huecoTomadoDevuelveAlternativasPosteriores() {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-5");
        crear(crearTokenReservar(pacienteId), new CrearPublicoRequest(pacienteId, profesionalId,
                sillonId, TRATAMIENTO, inicio, "idem-original"));

        PublicApiException excepcion = org.junit.jupiter.api.Assertions.assertThrows(
                PublicApiException.class,
                () -> crear(crearTokenReservar(pacienteId), new CrearPublicoRequest(pacienteId,
                        profesionalId, sillonId, TRATAMIENTO, inicio, "idem-conflicto")));

        assertEquals("hueco_tomado", excepcion.motivo());
        assertEquals(3, excepcion.alternativas().size());
        assertTrue(excepcion.alternativas().get(0).inicio().isAfter(inicio));
        assertEquals(1, turnos.count());
    }

    @Test
    void reprogramacionConservaEstadoYOrigen() {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-6");
        CrearTurnoPublicoResponse creado = crear(crearTokenReservar(pacienteId),
                new CrearPublicoRequest(pacienteId, profesionalId, sillonId, TRATAMIENTO,
                        inicio, "idem-crear-6"));
        servicio.confirmar(tokens.validar(creado.tokenGestionar()).claims(),
                new GestionarRequest(creado.turnoId(), "idem-confirmar-6"));

        Instant nuevoInicio = inicio.plusSeconds(1800);
        GestionarTurnoResponse reprogramado = servicio.reprogramar(
                tokens.validar(creado.tokenGestionar()).claims(),
                new ReprogramarRequest(creado.turnoId(), nuevoInicio, "idem-reprogramar-6"));

        assertEquals("confirmado", reprogramado.estado());
        assertEquals("link", turnos.findById(creado.turnoId()).orElseThrow().getOrigen());
        assertEquals(nuevoInicio, reprogramado.inicio());
        assertEquals(1, ocupaciones.countByActivoTrue());
        assertTrue(recorder.eventos().stream().anyMatch(evento -> "turno.reprogramado".equals(evento.tipo())
                && inicio.toString().equals(evento.metadata().get("inicioAnterior"))),
                "turno.reprogramado debe informar el inicio original (contrato con C-22)");
    }

    @Test
    void reprogramacionADestinoOcupadoDevuelveAlternativas() {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-9");
        CrearTurnoPublicoResponse primero = crear(crearTokenReservar(pacienteId),
                new CrearPublicoRequest(pacienteId, profesionalId, sillonId, TRATAMIENTO,
                        inicio, "idem-repro-original"));
        crear(crearTokenReservar(pacienteId), new CrearPublicoRequest(pacienteId, profesionalId,
                sillonId, TRATAMIENTO, inicio.plusSeconds(1800), "idem-repro-destino"));

        PublicApiException excepcion = org.junit.jupiter.api.Assertions.assertThrows(
                PublicApiException.class,
                () -> servicio.reprogramar(tokens.validar(primero.tokenGestionar()).claims(),
                        new ReprogramarRequest(primero.turnoId(), inicio.plusSeconds(1800),
                                "idem-repro-conflicto")));

        assertEquals("hueco_tomado", excepcion.motivo());
        assertEquals(2, excepcion.alternativas().size());
        assertEquals(inicio, turnos.findById(primero.turnoId()).orElseThrow().getInicio());
        assertEquals(2, ocupaciones.countByActivoTrue());
    }

    @Test
    void dosReservasConcurrentesSoloProducenUnaOcupacion() throws Exception {
        Long pacienteId = crearPaciente();
        Long profesionalId = crearProfesional();
        Long sillonId = crearSillon("S-PUB-7");
        String tokenUno = crearTokenReservar(pacienteId);
        String tokenDos = crearTokenReservar(pacienteId);
        CyclicBarrier barrera = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<Boolean> primera = () -> intentarCrear(barrera, tokenUno,
                    new CrearPublicoRequest(pacienteId, profesionalId, sillonId, TRATAMIENTO,
                            inicio, "idem-conc-1"));
            Callable<Boolean> segunda = () -> intentarCrear(barrera, tokenDos,
                    new CrearPublicoRequest(pacienteId, profesionalId, sillonId, TRATAMIENTO,
                            inicio, "idem-conc-2"));
            Future<Boolean> futuroUno = executor.submit(primera);
            Future<Boolean> futuroDos = executor.submit(segunda);

            int exitos = (futuroUno.get(30, TimeUnit.SECONDS) ? 1 : 0)
                    + (futuroDos.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, exitos);
            assertEquals(1, turnos.count());
            assertEquals(1, ocupaciones.countByActivoTrue());
            assertEquals(1, recorder.eventos().stream()
                    .filter(evento -> "turno.creado".equals(evento.tipo()))
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean intentarCrear(CyclicBarrier barrera, String token, CrearPublicoRequest solicitud)
            throws Exception {
        barrera.await();
        try {
            crear(token, solicitud);
            return true;
        } catch (PublicApiException | DataIntegrityViolationException | CannotAcquireLockException ex) {
            return false;
        }
    }

    private CrearTurnoPublicoResponse crear(String token, CrearPublicoRequest solicitud) {
        return servicio.crear(tokens.validar(token).claims(), solicitud);
    }

    private String crearTokenReservar(Long pacienteId) {
        return emitir(PublicTokenScope.RESERVAR, pacienteId, null);
    }

    private String emitir(PublicTokenScope scope, Long pacienteId, String turnoId) {
        long ahora = Instant.now().getEpochSecond();
        return tokens.emitir(new PublicTokenClaims("jti-" + UUID.randomUUID(), EMISOR,
                "paciente-" + pacienteId, String.valueOf(pacienteId), scope, ahora,
                ahora + scope.vigenciaSegundos(), turnoId, false));
    }

    private Long crearPaciente() {
        return pacientes.saveAndFlush(new Paciente(DocumentoTipo.DNI,
                "DNI-PUB-" + System.nanoTime(), "Paciente", "Público", "555")).getId();
    }

    private Long crearProfesional() {
        return profesionales.saveAndFlush(new com.turnosodontologia.domain.agenda.Profesional(
                "MP-PUB-" + System.nanoTime(), "Profesional", "Público")).getId();
    }

    private Long crearSillon(String numero) {
        return sillones.saveAndFlush(new com.turnosodontologia.domain.agenda.Sillon(numero,
                consultorios.findById(1L).orElseThrow())).getId();
    }

    private static LocalDate proximoDiaHabil(LocalDate candidato) {
        LocalDate dia = candidato;
        while (dia.getDayOfWeek() == DayOfWeek.SATURDAY
                || dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dia = dia.plusDays(1);
        }
        return dia;
    }
}
