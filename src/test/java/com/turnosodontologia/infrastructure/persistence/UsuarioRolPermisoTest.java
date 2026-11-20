package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.identidad.Ambito;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.domain.identidad.Permiso;
import com.turnosodontologia.domain.identidad.ProveedorAutenticacion;
import com.turnosodontologia.domain.identidad.Rol;
import com.turnosodontologia.domain.identidad.RolPermiso;
import com.turnosodontologia.domain.identidad.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UsuarioRolPermisoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RolPermisoRepository rolPermisoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void persisteUsuarioConRolYPermisoExplicito() {
        Rol rol = entityManager.persistAndFlush(new Rol("odontologo", "Odontólogo"));
        Permiso permiso = entityManager.persistAndFlush(new Permiso("agenda", "leer", Ambito.PROPIO));
        entityManager.persistAndFlush(new RolPermiso(rol, permiso));

        Usuario usuario = entityManager.persistAndFlush(new Usuario(
                rol, "odontologo@consultorio.test", "hash-local", ProveedorAutenticacion.LOCAL));

        assertEquals(rol.getId(), usuario.getRol().getId());
        assertEquals("hash-local", usuario.getHashPassword());
        assertEquals(1, rolPermisoRepository.findByRol(rol).size());
        assertEquals(1, usuarioRepository.count());
    }

    @Test
    void sinHerenciaImplícitaYLosDatosDelPacienteSeparadosDelUsuario() throws Exception {
        Rol odontologo = entityManager.persistAndFlush(new Rol("odontologo", "Odontólogo"));
        Permiso permiso = entityManager.persistAndFlush(new Permiso("auditoria", "leer", Ambito.TODOS));
        entityManager.persistAndFlush(new RolPermiso(odontologo, permiso));
        Rol paciente = entityManager.persistAndFlush(new Rol("paciente", "Paciente"));

        assertTrue(rolPermisoRepository.findByRol(paciente).isEmpty());
        assertEquals(1, rolPermisoRepository.findByRol(odontologo).size());
        assertThrows(NoSuchFieldException.class, () -> Usuario.class.getDeclaredField("documentoNumero"));
        assertThrows(NoSuchFieldException.class, () -> Paciente.class.getDeclaredField("rol"));
    }
}
