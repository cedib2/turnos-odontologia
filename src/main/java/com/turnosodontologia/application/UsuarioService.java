package com.turnosodontologia.application;

import com.turnosodontologia.domain.identidad.ProveedorAutenticacion;
import com.turnosodontologia.domain.identidad.Rol;
import com.turnosodontologia.domain.identidad.Usuario;
import com.turnosodontologia.infrastructure.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public Usuario registrar(Rol rol, String email, String hashPassword,
                             ProveedorAutenticacion proveedor) {
        return repository.saveAndFlush(new Usuario(rol, email, hashPassword, proveedor));
    }
}
