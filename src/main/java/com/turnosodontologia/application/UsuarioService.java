package com.turnosodontologia.application;

import com.turnosodontologia.auth.PasswordHasher;
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
    private final PasswordHasher hasher;

    public UsuarioService(UsuarioRepository repository, PasswordHasher hasher) {
        this.repository = repository;
        this.hasher = hasher;
    }

    public Usuario registrar(Rol rol, String email, String credencial,
                             ProveedorAutenticacion proveedor) {
        String almacenada = proveedor == ProveedorAutenticacion.LOCAL
                && credencial != null && !credencial.isBlank()
                ? hasher.hash(credencial)
                : credencial;
        return repository.saveAndFlush(new Usuario(rol, email, almacenada, proveedor));
    }
}
