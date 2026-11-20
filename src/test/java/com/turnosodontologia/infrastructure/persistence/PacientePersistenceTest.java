package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PacientePersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PacienteRepository repository;

    @Test
    void persistePacienteYConservaDatosDeAuditoria() {
        Paciente paciente = new Paciente(DocumentoTipo.DNI, "11222333", "Nora", "Díaz", "5491122233344");
        paciente.registrarCreacion("carga-inicial");

        Long id = (Long) entityManager.persistAndGetId(paciente);
        entityManager.flush();
        entityManager.clear();

        Paciente cargado = repository.findById(id).orElseThrow();
        assertEquals("11222333", cargado.getDocumentoNumero());
        assertEquals("5491122233344", cargado.getTelefono());
        assertEquals("carga-inicial", cargado.getCreatedBy());
        assertNotNull(cargado.getCreatedAt());
        assertTrue(cargado.isActivo());
    }
}
