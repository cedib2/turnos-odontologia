package com.turnosodontologia.interfaces.web;

import java.time.LocalTime;

public record HorarioRequest(Integer profesionalId, int diaSemana, LocalTime desde, LocalTime hasta) {
}
