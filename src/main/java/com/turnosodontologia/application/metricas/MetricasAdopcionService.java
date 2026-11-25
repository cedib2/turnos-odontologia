package com.turnosodontologia.application.metricas;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tablero de adopción (C-22): calcula cada métrica del catálogo con metodología y
 * período explícitos y registra la consulta en auditoría. Una métrica cuya fuente
 * todavía no existe se informa como {@link EstadoMetrica#NO_DISPONIBLE}, nunca con
 * una cifra decorativa. La consulta y su evento comparten transacción: el publicador de
 * C-05 toma {@code pg_advisory_xact_lock} y necesita una transacción activa.
 */
@Service
@Transactional
public class MetricasAdopcionService {

    public static final String EVENTO_CONSULTA = "metricas.consultadas";

    private final Map<String, FuenteMetrica> fuentes;
    private final AuditEventPublisher auditoria;
    private final CurrentActorProvider actores;

    public MetricasAdopcionService(List<FuenteMetrica> fuentes, AuditEventPublisher auditoria,
                                   CurrentActorProvider actores) {
        this.fuentes = fuentes.stream()
                .collect(Collectors.toUnmodifiableMap(FuenteMetrica::codigo, Function.identity()));
        this.auditoria = auditoria;
        this.actores = actores;
    }

    public List<ResultadoMetrica> consultar(Instant desde, Instant hasta) {
        Periodo periodo = new Periodo(desde, hasta);
        List<ResultadoMetrica> resultados = new ArrayList<>();
        for (CatalogoMetricas.Definicion definicion : CatalogoMetricas.DEFINICIONES) {
            FuenteMetrica fuente = fuentes.get(definicion.codigo());
            resultados.add(fuente == null
                    ? ResultadoMetrica.noDisponible(definicion.codigo(), periodo,
                            definicion.metodologia(), definicion.dependencia())
                    : fuente.calcular(periodo, definicion.metodologia()));
        }
        registrarConsulta(periodo, resultados);
        return List.copyOf(resultados);
    }

    private void registrarConsulta(Periodo periodo, List<ResultadoMetrica> resultados) {
        Map<String, Object> estados = new LinkedHashMap<>();
        resultados.forEach(r -> estados.put(r.codigo(), r.estado().name()));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("desde", periodo.desde().toString());
        metadata.put("hasta", periodo.hasta().toString());
        metadata.put("metricas", estados);
        auditoria.publish(AuditEvent.de(EVENTO_CONSULTA, "metricas", null, actores.current(),
                "consulta del tablero de adopción", metadata, UUID.randomUUID().toString()));
    }
}
