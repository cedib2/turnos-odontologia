package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Consultorio;
import com.turnosodontologia.infrastructure.persistence.ConsultorioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConsultorioService {

    private final ConsultorioRepository repository;

    public ConsultorioService(ConsultorioRepository repository) {
        this.repository = repository;
    }

    public Consultorio registrar(String nombre) {
        return repository.saveAndFlush(new Consultorio(nombre));
    }
}
