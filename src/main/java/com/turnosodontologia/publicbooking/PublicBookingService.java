package com.turnosodontologia.publicbooking;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.ConfiguracionSchedulingService;
import com.turnosodontologia.application.DisponibilidadService;
import com.turnosodontologia.application.IntervaloDisponible;
import com.turnosodontologia.domain.agenda.Ocupacion;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.OcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import com.turnosodontologia.publictoken.PublicIdempotencyStore;
import com.turnosodontologia.publictoken.PublicTokenClaims;
import com.turnosodontologia.publictoken.PublicTokenMetadataStore;
import com.turnosodontologia.publictoken.PublicTokenScope;
import com.turnosodontologia.publictoken.PublicTokenService;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PublicBookingService {

    private static final String ESTADO_RESERVADO = "reservado";
    private static final String ORIGEN_LINK = "link";
    private static final String AUTOR = "paciente";
    private static final int MAX_ALTERNATIVAS = 3;

    private final PacienteRepository pacientes;
    private final ProfesionalRepository profesionales;
    private final SillonRepository sillones;
    private final TratamientoRepository tratamientos;
    private final TurnoRepository turnos;
    private final OcupacionRepository ocupaciones;
    private final ConfiguracionSchedulingService configuracion;
    private final DisponibilidadService disponibilidad;
    private final VentanaHabilesService ventana;
    private final PublicTokenService tokens;
    private final PublicTokenMetadataStore metadatosToken;
    private final PublicIdempotencyStore idempotencia;
    private final AuditEventPublisher eventos;
    private final NotificacionPublicaPort notificaciones;

    public PublicBookingService(PacienteRepository pacientes,
                                ProfesionalRepository profesionales,
                                SillonRepository sillones,
                                TratamientoRepository tratamientos,
                                TurnoRepository turnos,
                                OcupacionRepository ocupaciones,
                                ConfiguracionSchedulingService configuracion,
                                DisponibilidadService disponibilidad,
                                VentanaHabilesService ventana,
                                PublicTokenService tokens,
                                PublicTokenMetadataStore metadatosToken,
                                PublicIdempotencyStore idempotencia,
                                AuditEventPublisher eventos,
                                NotificacionPublicaPort notificaciones) {
        this.pacientes = pacientes;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.tratamientos = tratamientos;
        this.turnos = turnos;
        this.ocupaciones = ocupaciones;
        this.configuracion = configuracion;
        this.disponibilidad = disponibilidad;
        this.ventana = ventana;
        this.tokens = tokens;
        this.metadatosToken = metadatosToken;
        this.idempotencia = idempotencia;
        this.eventos = eventos;
        this.notificaciones = notificaciones;
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidad(Long profesionalId, Long sillonId,
                                                 LocalDate fecha, String codigoTratamiento) {
        int duracion = configuracion.duracionMinutos(codigoTratamiento);
        List<HuecoResponse> huecos = disponibilidad.huecosLibres(profesionalId, sillonId, fecha, duracion)
                .stream()
                .map(hueco -> new HuecoResponse(hueco.inicio(), hueco.fin()))
                .toList();
        return new DisponibilidadResponse(profesionalId, sillonId, fecha.toString(),
                codigoTratamiento, huecos);
    }

    @Transactional
    public CrearTurnoPublicoResponse crear(PublicTokenClaims claims, CrearPublicoRequest solicitud) {
        registrarIdempotencia("turno.crear", solicitud.idempotencyKey());

        Long pacienteId = comoLong(claims.pacienteId(), "paciente_invalido");
        if (solicitud.pacienteId() != null && !pacienteId.equals(solicitud.pacienteId())) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "paciente_ajeno");
        }
        Long profesionalId = requerido(solicitud.profesionalId(), "profesionalId");
        Long sillonId = requerido(solicitud.sillonId(), "sillonId");
        String codigoTratamiento = requeridoTexto(solicitud.codigoTratamiento(), "codigoTratamiento");
        Instant inicio = requerido(solicitud.inicio(), "inicio");

        Paciente paciente = pacientes.findById(pacienteId)
                .orElseThrow(() -> new PublicApiException(HttpStatus.BAD_REQUEST, "paciente_inexistente"));
        Profesional profesional = profesionales.findById(profesionalId)
                .orElseThrow(() -> new PublicApiException(HttpStatus.BAD_REQUEST, "profesional_inexistente"));
        Sillon sillon = sillones.findById(sillonId)
                .orElseThrow(() -> new PublicApiException(HttpStatus.BAD_REQUEST, "sillon_inexistente"));
        Tratamiento tratamiento = tratamientos.findByCodigo(codigoTratamiento)
                .orElseThrow(() -> new PublicApiException(HttpStatus.BAD_REQUEST, "tratamiento_inexistente"));

        Instant fin = inicio.plus(tratamiento.getDuracionMinutos(), ChronoUnit.MINUTES);
        validarDestino(profesional.getId(), inicio, fin, codigoTratamiento);
        if (haySolapamiento(profesionalId, sillonId, inicio, fin, null)) {
            throw PublicApiException.huecoTomado(alternativas(profesionalId, tratamiento, inicio));
        }

        Turno turno = new Turno(paciente, profesional, sillon, tratamiento, inicio, fin,
                tratamiento.getDuracionMinutos(), ESTADO_RESERVADO, ORIGEN_LINK, null);
        turno.registrarCreacion(actor().nombre());
        Turno guardado;
        try {
            guardado = turnos.saveAndFlush(turno);
        } catch (DataIntegrityViolationException | CannotAcquireLockException ex) {
            throw PublicApiException.huecoTomado(List.of());
        }

        String jti = "jti-" + UUID.randomUUID();
        long emitido = tokens.reloj().instant().getEpochSecond();
        PublicTokenClaims gestionar = new PublicTokenClaims(jti, claims.iss(), claims.sub(),
                String.valueOf(pacienteId), PublicTokenScope.GESTIONAR, emitido,
                emitido + PublicTokenScope.GESTIONAR.vigenciaSegundos(),
                String.valueOf(guardado.getId()), false);
        String tokenGestionar = tokens.emitir(gestionar);

        guardado.asignarTokenPublico(jti, AUTOR);
        turnos.saveAndFlush(guardado);
        ocupaciones.saveAndFlush(Ocupacion.paraTurno(guardado.getId(), profesional.getId(),
                sillon.getId(), inicio, fin));

        metadatosToken.marcarUsado(claims.jti());
        publicar("turno.creado", guardado.getId(), solicitud.idempotencyKey(), null);
        notificaciones.solicitar(guardado.getId(), "turno.crear");

        return new CrearTurnoPublicoResponse(guardado.getId(), inicio, fin,
                guardado.getEstado(), guardado.getOrigen(), tokenGestionar,
                tratamiento.getNombre(), profesional.getNombre() + " " + profesional.getApellido(),
                etiquetaSillon(sillon.getNumero()), sillon.getConsultorio() == null ? null : sillon.getConsultorio().getNombre());
    }

    @Transactional
    public GestionarTurnoResponse confirmar(PublicTokenClaims claims, GestionarRequest solicitud) {
        registrarIdempotencia("turno.confirmar", solicitud.idempotencyKey());
        Turno turno = turnoGestionable(claims, solicitud.turnoId());
        // Confirmar la asistencia no está sujeto a la ventana de 48 hs: basta con que el turno no haya empezado.
        if (!ventana.antesDelInicio(turno.getInicio())) {
            throw PublicApiException.fueraDeVentana();
        }

        turno.confirmar(AUTOR);
        Turno guardado = turnos.saveAndFlush(turno);
        publicar("turno.confirmado", guardado.getId(), solicitud.idempotencyKey(), null);
        notificaciones.solicitar(guardado.getId(), "turno.confirmar");
        return respuesta(guardado);
    }

    @Transactional
    public GestionarTurnoResponse cancelar(PublicTokenClaims claims, GestionarRequest solicitud) {
        registrarIdempotencia("turno.cancelar", solicitud.idempotencyKey());
        Turno turno = turnoGestionable(claims, solicitud.turnoId());
        validarVentana(turno);

        Ocupacion ocupacion = ocupaciones.findByTurnoIdAndActivoTrue(turno.getId())
                .orElseThrow(() -> new IllegalStateException("El turno no tiene ocupación activa"));
        ocupacion.desactivar();
        ocupaciones.saveAndFlush(ocupacion);
        Instant inicioAnterior = turno.getInicio();
        turno.cancelar(AUTOR);
        Turno guardado = turnos.saveAndFlush(turno);

        publicar("turno.cancelado", guardado.getId(), solicitud.idempotencyKey(), null, inicioAnterior);
        notificaciones.solicitar(guardado.getId(), "turno.cancelar");
        return respuesta(guardado);
    }

    @Transactional
    public GestionarTurnoResponse reprogramar(PublicTokenClaims claims, ReprogramarRequest solicitud) {
        registrarIdempotencia("turno.reprogramar", solicitud.idempotencyKey());
        Turno turno = turnoGestionable(claims, solicitud.turnoId());
        validarVentana(turno);

        Instant nuevoInicio = requerido(solicitud.inicio(), "inicio");
        Instant nuevoFin = nuevoInicio.plus(turno.getDuracionMinutos(), ChronoUnit.MINUTES);
        Long profesionalId = turno.getProfesional().getId();
        Long sillonId = turno.getSillon().getId();
        validarDestino(profesionalId, nuevoInicio, nuevoFin, turno.getTratamiento().getCodigo());
        if (haySolapamiento(profesionalId, sillonId, nuevoInicio, nuevoFin, turno.getId())) {
            throw PublicApiException.huecoTomado(alternativas(profesionalId,
                    turno.getTratamiento(), nuevoInicio));
        }

        Ocupacion original = ocupaciones.findByTurnoIdAndActivoTrue(turno.getId())
                .orElseThrow(() -> new IllegalStateException("El turno no tiene ocupación activa"));
        original.desactivar();
        ocupaciones.saveAndFlush(original);

        Instant inicioAnterior = turno.getInicio();
        turno.reprogramar(nuevoInicio, nuevoFin, AUTOR);
        Turno guardado = turnos.saveAndFlush(turno);
        ocupaciones.saveAndFlush(Ocupacion.paraTurno(guardado.getId(), profesionalId, sillonId,
                nuevoInicio, nuevoFin));

        publicar("turno.reprogramado", guardado.getId(), solicitud.idempotencyKey(), "reprogramación",
                inicioAnterior);
        notificaciones.solicitar(guardado.getId(), "turno.reprogramar");
        return respuesta(guardado);
    }

    private void registrarIdempotencia(String operacion, String clave) {
        if (clave == null || clave.isBlank()) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "idempotency_key_requerida");
        }
        if (!idempotencia.registrar(operacion, clave)) {
            throw new PublicApiException(HttpStatus.CONFLICT, "operacion_duplicada");
        }
    }

    private Turno turnoGestionable(PublicTokenClaims claims, Long turnoId) {
        Long requerido = requerido(turnoId, "turnoId");
        Turno turno = turnos.findById(requerido)
                .orElseThrow(() -> new PublicApiException(HttpStatus.NOT_FOUND, "turno_inexistente"));
        if (!claims.jti().equals(turno.getTokenPublico())) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "token_no_vinculado");
        }
        if (!turno.isActivo()) {
            throw new PublicApiException(HttpStatus.CONFLICT, "turno_inactivo");
        }
        return turno;
    }

    private void validarVentana(Turno turno) {
        if (!ventana.dentroDeVentana(turno.getInicio())) {
            throw PublicApiException.fueraDeVentana();
        }
    }

    private void validarDestino(Long profesionalId, Instant inicio, Instant fin,
                                String codigoTratamiento) {
        LocalDate fecha = inicio.atZone(configuracion.zonaHoraria()).toLocalDate();
        if (configuracion.esFeriado(fecha)) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "feriado");
        }
        if (!configuracion.estaDentroDeHorario(profesionalId.intValue(), inicio, fin)) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, "fuera_de_horario");
        }
        configuracion.duracionMinutos(codigoTratamiento);
    }

    private boolean haySolapamiento(Long profesionalId, Long sillonId, Instant inicio, Instant fin,
                                    Long turnoExcluido) {
        return ocupaciones.findActivosQueSolapan(profesionalId, sillonId, inicio, fin)
                .stream()
                .anyMatch(ocupacion -> turnoExcluido == null
                        || !turnoExcluido.equals(ocupacion.getTurnoId()));
    }

    private List<HuecoResponse> alternativas(Long profesionalId, Tratamiento tratamiento, Instant desde) {
        LocalDate fecha = desde.atZone(configuracion.zonaHoraria()).toLocalDate();
        return disponibilidad.huecosLibres(profesionalId, null, fecha,
                        tratamiento.getDuracionMinutos())
                .stream()
                .filter(hueco -> !hueco.inicio().isBefore(desde))
                .limit(MAX_ALTERNATIVAS)
                .map(this::respuestaHueco)
                .toList();
    }

    private HuecoResponse respuestaHueco(IntervaloDisponible hueco) {
        return new HuecoResponse(hueco.inicio(), hueco.fin());
    }

    private GestionarTurnoResponse respuesta(Turno turno) {
        return new GestionarTurnoResponse(turno.getId(), turno.getEstado(), turno.isActivo(),
                turno.getInicio(), turno.getFin());
    }

    private void publicar(String tipo, Long turnoId, String idempotencyKey, String motivo) {
        publicar(tipo, turnoId, idempotencyKey, motivo, null);
    }

    private void publicar(String tipo, Long turnoId, String idempotencyKey, String motivo,
                          Instant inicioAnterior) {
        eventos.publish(AuditEvent.de(tipo, "turno", turnoId, actor(), motivo,
                PublicAuditMetadata.de(turnoId, idempotencyKey, motivo, inicioAnterior),
                UUID.randomUUID().toString()));
    }

    private ActorSimulado actor() {
        return ActorSimulado.pacienteSinUsuario();
    }

    private <T> T requerido(T valor, String nombre) {
        if (valor == null) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, nombre + "_requerido");
        }
        return valor;
    }

    private String requeridoTexto(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, nombre + "_requerido");
        }
        return valor;
    }

    private Long comoLong(String valor, String motivo) {
        try {
            return Long.valueOf(valor);
        } catch (RuntimeException ex) {
            throw new PublicApiException(HttpStatus.BAD_REQUEST, motivo);
        }
    }

    /** «Sillón 3» a partir del número, sin duplicar el prefijo si el número ya lo trae. */
    private static String etiquetaSillon(String numero) {
        if (numero == null) {
            return null;
        }
        String limpio = numero.trim();
        return limpio.toLowerCase(java.util.Locale.ROOT).matches("^sill[oó]n\\b.*") ? limpio : "Sillón " + limpio;
    }
}
