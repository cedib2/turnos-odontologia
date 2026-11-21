package com.turnosodontologia.interfaces.web;

import java.time.LocalDate;

public record FeriadoRequest(LocalDate fecha, String descripcion, Boolean aplicaATodos) {
}
