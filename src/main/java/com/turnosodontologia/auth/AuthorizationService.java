package com.turnosodontologia.auth;

import com.turnosodontologia.domain.identidad.Ambito;
import com.turnosodontologia.infrastructure.persistence.RolPermisoRepository;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationService {

    private final RolPermisoRepository rolPermisos;

    public AuthorizationService(RolPermisoRepository rolPermisos) {
        this.rolPermisos = rolPermisos;
    }

    public void exigirPropio(JwtService.AccessClaims claims, long recursoId) {
        if (claims.usuarioId() != recursoId || !tiene(claims.rol(), Ambito.PROPIO)) {
            throw new AccesoDenegadoException();
        }
    }

    public void exigirTodos(JwtService.AccessClaims claims) {
        if (!tiene(claims.rol(), Ambito.TODOS)) {
            throw new AccesoDenegadoException();
        }
    }

    private boolean tiene(String codigoRol, Ambito ambito) {
        return codigoRol != null && rolPermisos.existsByRol_CodigoAndPermiso_Ambito(codigoRol, ambito);
    }
}
