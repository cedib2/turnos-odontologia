package com.turnosodontologia.application;

import com.turnosodontologia.domain.agenda.Consultorio;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import com.turnosodontologia.domain.identidad.Ambito;
import com.turnosodontologia.domain.identidad.Permiso;
import com.turnosodontologia.domain.identidad.ProveedorAutenticacion;
import com.turnosodontologia.domain.identidad.Rol;
import com.turnosodontologia.domain.identidad.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Import({ConsultorioService.class, ProfesionalService.class, SillonService.class,
        RolService.class, PermisoService.class, UsuarioService.class})
class CoreServicesTest {

    @Autowired
    private ConsultorioService consultorioService;

    @Autowired
    private ProfesionalService profesionalService;

    @Autowired
    private SillonService sillonService;

    @Autowired
    private RolService rolService;

    @Autowired
    private PermisoService permisoService;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void registraCadaAgregadoEnTransaccion() {
        Consultorio consultorio = consultorioService.registrar("Consultorio principal");
        Profesional profesional = profesionalService.registrar("MP-77", "Clara", "Paz");
        Sillon sillon = sillonService.registrar("Sillón 1", consultorio);
        Rol rol = rolService.registrar("recepcionista", "Recepcionista");
        Permiso permiso = permisoService.registrar("agenda", "leer", Ambito.TODOS);
        Usuario usuario = usuarioService.registrar(
                rol, "recepcion@consultorio.test", "hash-local", ProveedorAutenticacion.LOCAL);

        assertNotNull(consultorio.getId());
        assertNotNull(profesional.getId());
        assertNotNull(sillon.getId());
        assertNotNull(rol.getId());
        assertNotNull(permiso.getId());
        assertNotNull(usuario.getId());
        assertEquals(consultorio.getId(), sillon.getConsultorio().getId());
        assertEquals(rol.getId(), usuario.getRol().getId());
    }

    @Test
    void rechazaUsuarioConEmailDuplicado() {
        Rol rol = rolService.registrar("administrador", "Administrador");
        usuarioService.registrar(rol, "admin@consultorio.test", "hash-1", ProveedorAutenticacion.LOCAL);

        assertThrows(DataIntegrityViolationException.class, () ->
                usuarioService.registrar(rol, "admin@consultorio.test", "hash-2", ProveedorAutenticacion.LOCAL));
    }
}
