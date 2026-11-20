package com.turnosodontologia.infrastructure.persistence;

import com.turnosodontologia.domain.identidad.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByDocumentoTipoAndDocumentoNumero(
            com.turnosodontologia.domain.identidad.DocumentoTipo documentoTipo, String documentoNumero);
}
