package com.turnosodontologia.interfaces.web;

import com.turnosodontologia.application.AgendaConsultaService;
import com.turnosodontologia.application.EntradaAgenda;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaConsultaService consulta;

    public AgendaController(AgendaConsultaService consulta) {
        this.consulta = consulta;
    }

    @GetMapping
    public List<EntradaAgenda> consultar(
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId,
            @RequestParam Instant desde,
            @RequestParam Instant hasta) {
        return consulta.consultar(profesionalId, sillonId, desde, hasta);
    }

    @GetMapping("/dia")
    public List<EntradaAgenda> porDia(
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId,
            @RequestParam LocalDate fecha) {
        return consulta.porDia(profesionalId, sillonId, fecha);
    }

    @GetMapping("/semana")
    public List<EntradaAgenda> porSemana(
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId,
            @RequestParam LocalDate inicio) {
        return consulta.porSemana(profesionalId, sillonId, inicio);
    }
}
