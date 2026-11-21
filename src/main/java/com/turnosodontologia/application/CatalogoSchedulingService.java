package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Feriado;
import com.turnosodontologia.domain.agenda.HorarioAtencion;
import com.turnosodontologia.domain.agenda.Tratamiento;
import com.turnosodontologia.infrastructure.persistence.FeriadoRepository;
import com.turnosodontologia.infrastructure.persistence.HorarioAtencionRepository;
import com.turnosodontologia.infrastructure.persistence.TratamientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class CatalogoSchedulingService {

    private final TratamientoRepository tratamientos;
    private final HorarioAtencionRepository horarios;
    private final FeriadoRepository feriados;

    public CatalogoSchedulingService(TratamientoRepository tratamientos,
                                     HorarioAtencionRepository horarios,
                                     FeriadoRepository feriados) {
        this.tratamientos = tratamientos;
        this.horarios = horarios;
        this.feriados = feriados;
    }

    public Tratamiento crearTratamiento(String codigo, String nombre, int duracionMinutos) {
        validarTratamiento(codigo, nombre);
        verificarCodigoDisponible(codigo, null);
        return tratamientos.save(new Tratamiento(codigo, nombre, duracionMinutos));
    }

    public List<Tratamiento> listarTratamientos() {
        return tratamientos.findAll();
    }

    public Tratamiento obtenerTratamiento(Long id) {
        return obtener(tratamientos.findById(id), "tratamiento " + id);
    }

    public Tratamiento actualizarTratamiento(Long id, String codigo, String nombre, int duracionMinutos) {
        Tratamiento tratamiento = obtenerTratamiento(id);
        validarTratamiento(codigo, nombre);
        verificarCodigoDisponible(codigo, id);
        tratamiento.actualizar(codigo, nombre, duracionMinutos);
        return tratamientos.save(tratamiento);
    }

    public Tratamiento eliminarTratamiento(Long id) {
        Tratamiento tratamiento = obtenerTratamiento(id);
        tratamiento.desactivar("system");
        return tratamientos.save(tratamiento);
    }

    public HorarioAtencion crearHorario(Integer profesionalId, int diaSemana,
                                        LocalTime desde, LocalTime hasta) {
        return horarios.save(new HorarioAtencion(profesionalId, diaSemana, desde, hasta));
    }

    public List<HorarioAtencion> listarHorarios() {
        return horarios.findAll();
    }

    public HorarioAtencion obtenerHorario(Long id) {
        return obtener(horarios.findById(id), "horario " + id);
    }

    public HorarioAtencion actualizarHorario(Long id, Integer profesionalId, int diaSemana,
                                             LocalTime desde, LocalTime hasta) {
        HorarioAtencion horario = obtenerHorario(id);
        horario.actualizar(profesionalId, diaSemana, desde, hasta);
        return horarios.save(horario);
    }

    public HorarioAtencion eliminarHorario(Long id) {
        HorarioAtencion horario = obtenerHorario(id);
        horario.desactivar("system");
        return horarios.save(horario);
    }

    public Feriado crearFeriado(LocalDate fecha, String descripcion, Boolean aplicaATodos) {
        Feriado feriado = new Feriado(fecha, descripcion, aplicaATodos);
        verificarFechaDisponible(fecha, null);
        return feriados.save(feriado);
    }

    public List<Feriado> listarFeriados() {
        return feriados.findAll();
    }

    public Feriado obtenerFeriado(Long id) {
        return obtener(feriados.findById(id), "feriado " + id);
    }

    public Feriado actualizarFeriado(Long id, LocalDate fecha, String descripcion, Boolean aplicaATodos) {
        Feriado feriado = obtenerFeriado(id);
        feriado.actualizar(fecha, descripcion, aplicaATodos);
        verificarFechaDisponible(fecha, id);
        return feriados.save(feriado);
    }

    public Feriado eliminarFeriado(Long id) {
        Feriado feriado = obtenerFeriado(id);
        feriado.desactivar("system");
        return feriados.save(feriado);
    }

    private static <T> T obtener(Optional<T> entidad, String nombre) {
        return entidad.orElseThrow(() -> new NoSuchElementException("No existe " + nombre));
    }

    private void validarTratamiento(String codigo, String nombre) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del tratamiento es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del tratamiento es obligatorio");
        }
    }

    private void verificarCodigoDisponible(String codigo, Long idActual) {
        tratamientos.findByCodigo(codigo).filter(otro -> !otro.getId().equals(idActual))
                .ifPresent(otro -> {
                    throw new DuplicadoCatalogoException(
                            "Ya existe un tratamiento con código " + codigo);
                });
    }

    private void verificarFechaDisponible(LocalDate fecha, Long idActual) {
        feriados.findByFecha(fecha).filter(otro -> !otro.getId().equals(idActual))
                .ifPresent(otro -> {
                    throw new DuplicadoCatalogoException(
                            "Ya existe un feriado para la fecha " + fecha);
                });
    }
}
