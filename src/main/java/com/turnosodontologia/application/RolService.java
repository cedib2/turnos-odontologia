package com.turnosodontologia.application;

import com.turnosodontologia.domain.identidad.Rol;
import com.turnosodontologia.infrastructure.persistence.RolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RolService {

    private final RolRepository repository;

    public RolService(RolRepository repository) {
        this.repository = repository;
    }

    public Rol registrar(String codigo, String nombre) {
        return repository.saveAndFlush(new Rol(codigo, nombre));
    }
}
