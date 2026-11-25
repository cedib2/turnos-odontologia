package com.turnosodontologia.export;

import java.time.Instant;

public record BackupObjeto(String tenantId, String artefactoId, byte[] contenidoCifrado,
                           String sha256, Instant guardadoEn, boolean expiradoLogico) {
}
