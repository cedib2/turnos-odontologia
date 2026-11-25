package com.turnosodontologia.application.cumplimiento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Base descartable donde se prueba cada restore antes de tocar datos de usuario (RN-SE-03, C-17).
 * Se configura con {@code RESTORE_SANDBOX_URL}/{@code _USER}/{@code _PASSWORD}; nunca puede ser la base productiva.
 */
@Component
public class EntornoAisladoRestore {

    private final String url;
    private final String usuario;
    private final String clave;

    public EntornoAisladoRestore(
            @Value("${cumplimiento.restore.aislado.url:${RESTORE_SANDBOX_URL:}}") String url,
            @Value("${cumplimiento.restore.aislado.usuario:${RESTORE_SANDBOX_USER:}}") String usuario,
            @Value("${cumplimiento.restore.aislado.clave:${RESTORE_SANDBOX_PASSWORD:}}") String clave) {
        this.url = url == null || url.isBlank() ? null : url.trim();
        this.usuario = usuario;
        this.clave = clave;
    }

    public boolean configurado() {
        return url != null;
    }

    /** Descriptor que se registra: la URL sin parámetros (que podrían llevar credenciales). */
    public String descriptor() {
        if (url == null) {
            return "(no configurado)";
        }
        int consulta = url.indexOf('?');
        return consulta < 0 ? url : url.substring(0, consulta);
    }

    public String url() {
        return url;
    }

    public String usuario() {
        return usuario;
    }

    public String clave() {
        return clave;
    }

    public Connection abrir() throws SQLException {
        if (url == null) {
            throw new IllegalStateException("Entorno aislado de restore no configurado (RESTORE_SANDBOX_URL)");
        }
        return DriverManager.getConnection(url, usuario, clave);
    }
}
