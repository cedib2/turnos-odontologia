package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.AccesoFueraDeAlcanceException;
import com.turnosodontologia.application.AgendaConsultaService;
import com.turnosodontologia.application.AlcanceProfesional;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.application.EntradaAgenda;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/agenda")
public class AgendaController {

    /** Permisos de lectura de agenda del seed V001: {@code agenda:leer} y {@code turno:leer}. */
    static final Set<String> RECURSOS_AGENDA = Set.of("agenda", "turno");

    private final AgendaConsultaService consulta;
    private final AlcanceProfesional alcances;
    private final CurrentActorProvider actores;

    public AgendaController(AgendaConsultaService consulta, AlcanceProfesional alcances,
                            CurrentActorProvider actores) {
        this.consulta = consulta;
        this.alcances = alcances;
        this.actores = actores;
    }

    @GetMapping
    public List<EntradaAgenda> consultar(
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId,
            @RequestParam Instant desde,
            @RequestParam Instant hasta) {
        return consulta.consultar(profesional(profesionalId), sillonId, desde, hasta);
    }

    @GetMapping("/dia")
    public List<EntradaAgenda> porDia(
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId,
            @RequestParam LocalDate fecha) {
        return consulta.porDia(profesional(profesionalId), sillonId, fecha);
    }

    @GetMapping("/semana")
    public List<EntradaAgenda> porSemana(
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId,
            @RequestParam LocalDate inicio) {
        return consulta.porSemana(profesional(profesionalId), sillonId, inicio);
    }

    /**
     * Ámbito {@code propio} (odontólogo): solo su agenda. Sin filtro se usa la suya; pedir la de otro
     * profesional se rechaza. Con ámbito {@code todos} se respeta el filtro pedido.
     */
    private Long profesional(Long pedido) {
        AlcanceProfesional.Alcance alcance = alcances.de(actores.current(), RECURSOS_AGENDA, "leer");
        if (alcance.todos()) {
            return pedido;
        }
        if (pedido != null && !pedido.equals(alcance.profesionalId())) {
            throw new AccesoFueraDeAlcanceException("agenda_de_otro_profesional");
        }
        return alcance.profesionalId();
    }
}
