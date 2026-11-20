package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Profesional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ProfesionalPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProfesionalRepository repository;

    @Test
    void findByActivoTrueIncluyeSoloProfesionalesActivos() {
        Profesional activo = entityManager.persistAndFlush(new Profesional("MP-1", "Sofía", "Aguirre"));
        Profesional inactivo = entityManager.persistAndFlush(new Profesional("MP-2", "Diego", "Soto"));
        inactivo.desactivar("admin");
        entityManager.flush();

        assertTrue(repository.findByActivoTrue().stream().anyMatch(p -> p.getId().equals(activo.getId())));
        assertFalse(repository.findByActivoTrue().stream().anyMatch(p -> p.getId().equals(inactivo.getId())));
        assertEquals("MP-2", repository.findByMatricula("MP-2").orElseThrow().getMatricula());
    }

    @Test
    void rechazaMatriculaDuplicada() {
        repository.saveAndFlush(new Profesional("MP-9", "Única", "Dueño"));
        Profesional duplicado = new Profesional("MP-9", "Otra", "Persona");

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicado));
    }
}
