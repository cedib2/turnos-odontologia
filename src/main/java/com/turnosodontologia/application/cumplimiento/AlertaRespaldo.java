package com.turnosodontologia.application.cumplimiento;

/** Canal de alerta ante fallo de respaldo o restore. El canal real (mail, WhatsApp, guardia) sigue (TBD). */
public interface AlertaRespaldo {

    void notificarFallo(OperacionCumplimiento operacion);
}
