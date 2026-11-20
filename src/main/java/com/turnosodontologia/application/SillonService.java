package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Consultorio;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.infrastructure.persistence.SillonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SillonService {

    private final SillonRepository repository;

    public SillonService(SillonRepository repository) {
        this.repository = repository;
    }

    public Sillon registrar(String numero, Consultorio consultorio) {
        return repository.saveAndFlush(new Sillon(numero, consultorio));
    }
}
