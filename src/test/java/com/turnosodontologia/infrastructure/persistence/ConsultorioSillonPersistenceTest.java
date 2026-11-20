package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.agenda.Consultorio;
import com.turnosodontologia.domain.agenda.Profesional;
import com.turnosodontologia.domain.agenda.Sillon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ConsultorioSillonPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private SillonRepository sillonRepository;

    @Test
    void persisteConsultorioUnicoYSillonAsociado() {
        Consultorio consultorio = entityManager.persistAndFlush(new Consultorio("Consultorio principal"));
        Sillon sillon = entityManager.persistAndFlush(new Sillon("Sillón 1", consultorio));

        assertEquals(1, consultorioRepository.count());
        assertEquals(consultorio.getId(), sillon.getConsultorio().getId());
        assertTrue(sillonRepository.findByActivoTrue().stream().anyMatch(s -> s.getId().equals(sillon.getId())));
    }

    @Test
    void excluyeSillonInactivoDeLaBusquedaActiva() {
        Consultorio consultorio = entityManager.persistAndFlush(new Consultorio("Consultorio principal"));
        Sillon inactivo = entityManager.persistAndFlush(new Sillon("Sillón 2", consultorio));
        inactivo.desactivar("admin");
        entityManager.flush();

        assertFalse(sillonRepository.findByActivoTrue().stream().anyMatch(s -> s.getId().equals(inactivo.getId())));
    }

    @Test
    void modeloNoContieneAislamientoPorSede() throws Exception {
        assertThrows(NoSuchFieldException.class, () -> Consultorio.class.getDeclaredField("sedeId"));
        assertThrows(NoSuchFieldException.class, () -> Sillon.class.getDeclaredField("sedeId"));
        assertThrows(NoSuchFieldException.class, () -> Profesional.class.getDeclaredField("sedeId"));
    }
}
