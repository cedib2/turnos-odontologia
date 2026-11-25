package com.turnosodontologia.publicbooking;

public interface NotificacionPublicaPort {

    void solicitar(Long turnoId, String operacion);
}
