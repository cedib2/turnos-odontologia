package com.turnosodontologia.application;

import com.turnosodontologia.domain.identidad.DocumentoTipo;
import com.turnosodontologia.domain.identidad.Paciente;
import com.turnosodontologia.infrastructure.persistence.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Import(PacienteService.class)
class PacienteServiceTest {

    @Autowired
    private PacienteService service;

    @Autowired
    private PacienteRepository repository;

    @Test
    void registraPacienteEnTransaccion() {
        Paciente guardado = service.registrar(DocumentoTipo.DNI, "99887766", "Luis", "López", "5491188887777");

        assertTrue(guardado.getId() > 0);
        assertEquals("99887766", repository.findById(guardado.getId()).orElseThrow().getDocumentoNumero());
    }

    @Test
    void rechazaDocumentoDuplicado() {
        service.registrar(DocumentoTipo.DNI, "44556677", "Ana", "Ruiz", "5491177776666");

        assertThrows(DataIntegrityViolationException.class, () ->
                service.registrar(DocumentoTipo.DNI, "44556677", "Otra", "Persona", "5491166665555"));
    }

    @Test
    void rechazaPacienteSinTelefono() {
        assertThrows(DataIntegrityViolationException.class, () ->
                service.registrar(DocumentoTipo.PASAPORTE, "X123456", "Sin", "Telefono", null));
    }
}
