package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Feriado;
import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import com.turnosodontologia.infrastructure.persistence.FeriadoRepository;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import com.turnosodontologia.support.PostgresFlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisponibilidadIntegrationTest extends PostgresFlywayTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 23);
    private static final Instant TRECE = FECHA.atTime(13, 0)
            .atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant();

    @Autowired
    private DisponibilidadService service;

    @Autowired
    private BloqueoService bloqueos;

    @Autowired
    private HorarioAtencionRepository horarios;

    @Autowired
    private FeriadoRepository feriados;

    @Autowired
    private SillonRepository sillones;

    @Autowired
    private ConsultorioRepository consultorios;

    @BeforeEach
    void sembrarHorario() {
        horarios.saveAndFlush(new HorarioAtencion(null, FECHA.getDayOfWeek().getValue(),
                LocalTime.of(13, 0), LocalTime.of(14, 30)));
    }

    @Test
    void noOfreceElHuecoOcupadoPorUnBloqueo() {
        Long sillon = crearSillon("S-DISP-INT-1");
        bloqueos.crearBloqueo(null, sillon, TRECE, TRECE.plusSeconds(1800), "fixture-motivo");

        List<IntervaloDisponible> libres = service.huecosLibres(null, sillon, FECHA, 30);

        assertFalse(libres.stream().anyMatch(hueco -> hueco.inicio().equals(TRECE)));
        assertTrue(libres.stream().anyMatch(hueco ->
                hueco.inicio().equals(TRECE.plusSeconds(1800))));
    }

    @Test
    void noOfreceHuecosEnFeriado() {
        Long sillon = crearSillon("S-DISP-INT-2");
        feriados.saveAndFlush(new Feriado(FECHA, "fixture-feriado"));

        assertTrue(service.huecosLibres(null, sillon, FECHA, 30).isEmpty());
    }

    private Long crearSillon(String numero) {
        Sillon sillon = sillones.saveAndFlush(new Sillon(numero,
                consultorios.findById(1L).orElseThrow()));
        return sillon.getId();
    }
}
