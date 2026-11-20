package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.infrastructure.persistence.ProfesionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfesionalService {

    private final ProfesionalRepository repository;

    public ProfesionalService(ProfesionalRepository repository) {
        this.repository = repository;
    }

    public Profesional registrar(String matricula, String nombre, String apellido) {
        return repository.saveAndFlush(new Profesional(matricula, nombre, apellido));
    }
}
