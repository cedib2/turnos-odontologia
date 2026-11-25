package com.turnosodontologia.export;

public interface BackupStoragePort {

    void guardar(String tenantId, String artefactoId, byte[] contenido);

    byte[] leer(String tenantId, String artefactoId);

    java.util.List<String> listar(String tenantId);
}
