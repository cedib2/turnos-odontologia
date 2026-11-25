package com.turnosodontologia.waitlist;

import com.turnosodontologia.application.ConfiguracionSchedulingService;
import com.turnosodontologia.domain.agenda.Bloqueo;
import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Turno;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import com.turnosodontologia.domain.waitlist.MetricaOcupacion;
import com.turnosodontologia.infrastructure.persistence.BloqueoRepository;
import com.turnosodontologia.infrastructure.persistence.MetricaOcupacionRepository;
import com.turnosodontologia.infrastructure.persistence.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cálculo auditable de ocupación y ausentismo (W-9). Bloqueos cuentan como
 * ocupación; cancelados nunca cuentan como inasistencia. La hora de cálculo
 * sale del {@link Clock} inyectado; el período se resuelve en zona fija.
 */
@Service
@Transactional
public class MetricaOcupacionService {

    private static final Pattern PERIODO = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");
    private static final String METODOLOGIA =
            "ocupacion=100*minutos(turnos activos+bloqueos activos)/minutos(horarios activos menos feriados); "
                    + "ausentismo=100*turnos inasistentes/turnos confirmados (cancelados excluidos); "
                    + "fuente=turnos,bloqueos,horarios,feriados; zona=America/Argentina/Buenos_Aires; "
                    + "periodo=YYYY-MM; meta=(TBD) -> forma de obtencion (piloto y PO Parte 3)";

    private final TurnoRepository turnos;
    private final BloqueoRepository bloqueos;
    private final MetricaOcupacionRepository metricas;
    private final ConfiguracionSchedulingService configuracion;
    private final WaitlistAuditPublisher auditoria;
    private final Clock reloj;

    public MetricaOcupacionService(TurnoRepository turnos, BloqueoRepository bloqueos,
                                   MetricaOcupacionRepository metricas,
                                   ConfiguracionSchedulingService configuracion,
                                   WaitlistAuditPublisher auditoria, Clock reloj) {
        this.turnos = turnos;
        this.bloqueos = bloqueos;
        this.metricas = metricas;
        this.configuracion = configuracion;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /** Publica la métrica; con denominador cero no persiste cifra ni emite evento. */
    public MetricaOcupacion publicar(Long sillonId, String periodo) {
        validar(sillonId, periodo);
        Intervalo intervalo = intervalo(periodo);
        long disponibles = minutosDisponibles(periodo);
        if (disponibles == 0) {
            return null;
        }
        BigDecimal ocupacion = porcentaje(minutosOcupados(sillonId, intervalo), disponibles);
        ConteoAusentismo ausentismo = calcularAusentismo(sillonId, intervalo);
        ActorSimulado actor = ActorSimulado.system();

        MetricaOcupacion metrica = metricas.findBySillonIdAndPeriodoAndActivoTrue(sillonId, periodo)
                .orElse(null);
        if (metrica == null) {
            metrica = new MetricaOcupacion(sillonId, periodo, ocupacion, ausentismo.valor(),
                    METODOLOGIA, reloj.instant());
            metrica.registrarCreacion(actor.nombre());
        } else {
            metrica.actualizar(ocupacion, ausentismo.valor(), METODOLOGIA, reloj.instant(),
                    actor.nombre());
        }
        MetricaOcupacion guardada = metricas.saveAndFlush(metrica);
        auditoria.publicar("metrica.publicada", "metrica", guardada.getId(), actor, "publicacion",
                WaitlistAuditMetadata.de(UUID.randomUUID().toString())
                        .metrica(guardada.getId()).sillon(sillonId).periodo(periodo)
                        .ocupacion(ocupacion.toPlainString())
                        .ausentismo(ausentismo.valor() == null ? null : ausentismo.valor().toPlainString())
                        .metodologia(METODOLOGIA));
        return guardada;
    }

    @Transactional(readOnly = true)
    public MetricaOcupacion consultar(Long sillonId, String periodo) {
        validar(sillonId, periodo);
        return metricas.findBySillonIdAndPeriodoAndActivoTrue(sillonId, periodo).orElse(null);
    }

    /** La inasistencia se registra explícitamente; nunca se infiere de una cancelación. */
    public Turno registrarInasistencia(Long turnoId) {
        if (turnoId == null) {
            throw new IllegalArgumentException("El turno es obligatorio");
        }
        Turno turno = turnos.findById(turnoId)
                .orElseThrow(() -> new NoSuchElementException("Turno inexistente: " + turnoId));
        ActorSimulado actor = ActorSimulado.recepcionista();
        turno.registrarInasistencia(actor.nombre());
        turnos.saveAndFlush(turno);
        auditoria.publicar("turno.inasistencia", "turno", turno.getId(), actor, "inasistencia",
                WaitlistAuditMetadata.de(UUID.randomUUID().toString())
                        .turno(turnoId).sillon(turno.getSillon() == null ? null : turno.getSillon().getId())
                        );
        return turno;
    }

    private void validar(Long sillonId, String periodo) {
        if (sillonId == null) {
            throw new IllegalArgumentException("El sillón es obligatorio");
        }
        if (periodo == null || !PERIODO.matcher(periodo).matches()) {
            throw new IllegalArgumentException("El período debe tener formato YYYY-MM");
        }
    }

    private Intervalo intervalo(String periodo) {
        YearMonth mes = YearMonth.parse(periodo);
        ZoneId zona = configuracion.zonaHoraria();
        Instant desde = mes.atDay(1).atStartOfDay(zona).toInstant();
        Instant hasta = mes.plusMonths(1).atDay(1).atStartOfDay(zona).toInstant();
        return new Intervalo(desde, hasta);
    }

    private long minutosDisponibles(String periodo) {
        YearMonth mes = YearMonth.parse(periodo);
        long total = 0;
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate fecha = mes.atDay(dia);
            if (configuracion.esFeriado(fecha)) {
                continue;
            }
            for (HorarioAtencion horario : configuracion.horariosDelDia(null, fecha)) {
                if (horario.isActivo()) {
                    total += Duration.between(horario.getDesde(), horario.getHasta()).toMinutes();
                }
            }
        }
        return total;
    }

    private long minutosOcupados(Long sillonId, Intervalo intervalo) {
        long total = 0;
        for (Turno turno : turnos.findActivosEnRango(null, sillonId,
                intervalo.desde(), intervalo.hasta())) {
            total += recortar(turno.getInicio(), turno.getFin(), intervalo);
        }
        for (Bloqueo bloqueo : bloqueos.findActivosEnRango(null, sillonId,
                intervalo.desde(), intervalo.hasta())) {
            total += recortar(bloqueo.getDesde(), bloqueo.getHasta(), intervalo);
        }
        return total;
    }

    private long recortar(Instant desde, Instant hasta, Intervalo intervalo) {
        Instant inicio = desde.isAfter(intervalo.desde()) ? desde : intervalo.desde();
        Instant fin = hasta.isBefore(intervalo.hasta()) ? hasta : intervalo.hasta();
        return fin.isAfter(inicio) ? Duration.between(inicio, fin).toMinutes() : 0L;
    }

    private ConteoAusentismo calcularAusentismo(Long sillonId, Intervalo intervalo) {
        List<Turno> candidatos = turnos.findActivosEnRango(null, sillonId,
                intervalo.desde(), intervalo.hasta());
        long inasistencias = candidatos.stream()
                .filter(turno -> "inasistente".equals(turno.getEstado())).count();
        long confirmados = candidatos.stream()
                .filter(turno -> "confirmado".equals(turno.getEstado())
                        || "inasistente".equals(turno.getEstado())).count();
        return new ConteoAusentismo(confirmados == 0 ? null : porcentaje(inasistencias, confirmados));
    }

    private BigDecimal porcentaje(long numerador, long denominador) {
        if (denominador == 0) {
            return null;
        }
        return BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(numerador))
                .divide(BigDecimal.valueOf(denominador), 4, RoundingMode.HALF_UP);
    }

    private record Intervalo(Instant desde, Instant hasta) {
    }

    private record ConteoAusentismo(BigDecimal valor) {
    }
}
