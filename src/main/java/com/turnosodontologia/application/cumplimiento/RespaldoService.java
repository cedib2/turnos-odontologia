package com.turnosodontologia.application.cumplimiento;

import com.turnosodontologia.application.AuditEventPublisher;
import com.turnosodontologia.application.CurrentActorProvider;
import com.turnosodontologia.domain.auditoria.ActorSimulado;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Respaldo completo cifrado y restore verificado en entorno aislado (C-17, RN-SE-03, RN-SE-05).
 * El restore nunca escribe sobre la base productiva: se prueba en la base descartable y se compara fila a fila.
 */
@Service
public class RespaldoService {

    private static final Logger LOG = LoggerFactory.getLogger(RespaldoService.class);

    private final DataSource dataSource;
    private final RegistroOperaciones registro;
    private final CifradorArtefactos cifrador;
    private final AlmacenArtefactos almacen;
    private final ParametrosCumplimiento parametros;
    private final EntornoAisladoRestore entornoAislado;
    private final CurrentActorProvider actores;
    private final AuditEventPublisher publisher;
    private final TransactionTemplate tx;
    private final ObjectProvider<AlertaRespaldo> alertas;
    private final Clock clock;

    public RespaldoService(DataSource dataSource, RegistroOperaciones registro, CifradorArtefactos cifrador,
                           AlmacenArtefactos almacen, ParametrosCumplimiento parametros,
                           EntornoAisladoRestore entornoAislado, CurrentActorProvider actores,
                           AuditEventPublisher publisher, PlatformTransactionManager transactionManager,
                           ObjectProvider<AlertaRespaldo> alertas, ObjectProvider<Clock> clock) {
        this.dataSource = dataSource;
        this.registro = registro;
        this.cifrador = cifrador;
        this.almacen = almacen;
        this.parametros = parametros;
        this.entornoAislado = entornoAislado;
        this.actores = actores;
        this.publisher = publisher;
        this.tx = new TransactionTemplate(transactionManager);
        this.alertas = alertas;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    /** Respaldo pedido por un usuario de staff. */
    public OperacionCumplimiento respaldar(String motivo) {
        return respaldar(actores.current(), motivo);
    }

    /** Respaldo disparado por {@code BACKUP_CRON}, con actor de sistema. */
    public OperacionCumplimiento respaldarProgramado() {
        return respaldar(actores.system(), "respaldo programado");
    }

    private OperacionCumplimiento respaldar(ActorSimulado actor, String motivo) {
        Instant inicio = clock.instant();
        long id = tx.execute(estado -> registro.solicitar(OperacionCumplimiento.RESPALDO,
                OperacionCumplimiento.COMPLETA, null, Paquete.FORMATO, motivo, actor.nombre(), inicio,
                null, null, null));
        try {
            Paquete paquete;
            try (Connection conexion = dataSource.getConnection()) {
                paquete = VolcadoBaseDatos.volcar(conexion, OperacionCumplimiento.RESPALDO,
                        OperacionCumplimiento.COMPLETA, inicio.toString(), VolcadoBaseDatos.Seleccion.completa(),
                        Map.of());
            }
            byte[] plano = paquete.serializar();
            String checksum = Paquete.sha256(plano);
            String destino = almacen.guardar("respaldo-" + id + "-" + java.util.UUID.randomUUID() + ".bin", cifrador.cifrar(plano));
            Instant fin = clock.instant();
            return tx.execute(estado -> {
                registro.completar(id, destino, true, checksum, paquete.tablas().size(), paquete.totalFilas(),
                        fin, parametros.retencionDesde(fin));
                OperacionCumplimiento completada = registro.buscar(id).orElseThrow();
                CumplimientoAuditoria.publicar(publisher, actor, "respaldo.completado", completada, motivo);
                return completada;
            });
        } catch (SQLException | RuntimeException e) {
            return fallar(id, actor, "respaldo.fallido", e, motivo);
        }
    }

    /**
     * Restaura el respaldo indicado en el entorno aislado y verifica que cada tabla coincide con el respaldo.
     * Rechaza el entorno si resulta ser la misma base que la productiva.
     */
    public OperacionCumplimiento restaurarEnAislado(long respaldoId, String motivo) {
        OperacionCumplimiento respaldo = registro.buscar(respaldoId)
                .filter(o -> OperacionCumplimiento.RESPALDO.equals(o.tipo()) && o.completada())
                .orElseThrow(() -> new IllegalArgumentException("Respaldo no disponible: " + respaldoId));
        ActorSimulado actor = actores.current();
        Instant inicio = clock.instant();
        long id = tx.execute(estado -> registro.solicitar(OperacionCumplimiento.RESTORE,
                OperacionCumplimiento.COMPLETA, null, respaldo.formato(), motivo, actor.nombre(), inicio,
                null, respaldoId, entornoAislado.descriptor()));
        try {
            exigirAislamiento();
            byte[] plano = cifrador.descifrar(almacen.leer(respaldo.destino()));
            if (!Paquete.sha256(plano).equals(respaldo.checksumSha256())) {
                throw new IllegalStateException("El respaldo " + respaldoId + " no coincide con su checksum");
            }
            Paquete paquete = Paquete.deserializar(plano);
            Flyway flyway = Flyway.configure()
                    .dataSource(entornoAislado.url(), entornoAislado.usuario(), entornoAislado.clave())
                    .locations("classpath:db/migration")
                    .outOfOrder(true)
                    .cleanDisabled(false)
                    .load();
            flyway.clean();
            flyway.migrate();
            Map<String, Integer> verificadas;
            try (Connection destino = entornoAislado.abrir()) {
                verificadas = VolcadoBaseDatos.restaurarYVerificar(destino, paquete);
            }
            long filas = verificadas.values().stream().mapToLong(Integer::longValue).sum();
            Instant fin = clock.instant();
            return tx.execute(estado -> {
                registro.completar(id, entornoAislado.descriptor(), false, respaldo.checksumSha256(),
                        verificadas.size(), filas, fin, null);
                OperacionCumplimiento completada = registro.buscar(id).orElseThrow();
                CumplimientoAuditoria.publicar(publisher, actor, "restore.verificado", completada, motivo);
                return completada;
            });
        } catch (SQLException | RuntimeException e) {
            return fallar(id, actor, "restore.fallido", e, motivo);
        }
    }

    private void exigirAislamiento() throws SQLException {
        String productiva;
        try (Connection conexion = dataSource.getConnection()) {
            productiva = VolcadoBaseDatos.identidad(conexion);
        }
        String aislada;
        try (Connection conexion = entornoAislado.abrir()) {
            aislada = VolcadoBaseDatos.identidad(conexion);
        }
        if (productiva.equals(aislada)) {
            throw new IllegalStateException(
                    "El entorno aislado es la base productiva: restore rechazado para no escribir sobre datos de usuario");
        }
    }

    private OperacionCumplimiento fallar(long id, ActorSimulado actor, String evento, Exception e, String motivo) {
        OperacionCumplimiento fallida = tx.execute(estado -> {
            registro.fallar(id, ExportacionService.mensaje(e), clock.instant());
            OperacionCumplimiento operacion = registro.buscar(id).orElseThrow();
            CumplimientoAuditoria.publicar(publisher, actor, evento, operacion, motivo);
            return operacion;
        });
        AlertaRespaldo alerta = alertas.getIfAvailable();
        if (alerta == null) {
            LOG.error("ALERTA {} operación {}: {}", evento, id, fallida.error());
        } else {
            alerta.notificarFallo(fallida);
        }
        return fallida;
    }
}
