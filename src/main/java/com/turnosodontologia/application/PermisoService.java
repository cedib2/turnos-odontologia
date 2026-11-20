package com.turnosodontologia.application;

import com.turnosodontologia.domain.identidad.Ambito;
import com.turnosodontologia.domain.identidad.Permiso;
import com.turnosodontologia.infrastructure.persistence.PermisoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PermisoService {

    private final PermisoRepository repository;

    public PermisoService(PermisoRepository repository) {
        this.repository = repository;
    }

    public Permiso registrar(String recurso, String accion, Ambito ambito) {
        return repository.saveAndFlush(new Permiso(recurso, accion, ambito));
    }
}
