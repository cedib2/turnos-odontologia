package com.turnosodontologia.application;

import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PacienteService {

    private final PacienteRepository repository;

    public PacienteService(PacienteRepository repository) {
        this.repository = repository;
    }

    public Paciente registrar(DocumentoTipo documentoTipo, String documentoNumero,
                              String nombre, String apellido, String telefono) {
        return repository.saveAndFlush(
                new Paciente(documentoTipo, documentoNumero, nombre, apellido, telefono));
    }
}
