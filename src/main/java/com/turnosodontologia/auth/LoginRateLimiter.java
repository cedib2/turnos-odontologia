package com.turnosodontologia.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private final Clock reloj;
    private final int maximoIntentos;
    private final Duration ventana;
    private final Map<String, Ventana> combinaciones = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(@Value("${auth.rate-limit.max-intentos:5}") int maximoIntentos,
                            @Value("${auth.rate-limit.ventana-minutos:15}") long ventanaMinutos) {
        this(Clock.systemUTC(), maximoIntentos, ventanaMinutos);
    }

    LoginRateLimiter(Clock reloj, int maximoIntentos, long ventanaMinutos) {
        this.reloj = reloj;
        this.maximoIntentos = maximoIntentos;
        this.ventana = Duration.ofMinutes(ventanaMinutos);
    }

    public Decision evaluar(String ip, String usuario) {
        synchronized (combinaciones) {
            String clave = clave(ip, usuario);
            Ventana actual = combinaciones.get(clave);
            if (actual == null) {
                return Decision.permitido();
            }
            Instant ahora = reloj.instant();
            if (!ahora.isBefore(actual.fin())) {
                combinaciones.remove(clave);
                return Decision.permitido();
            }
            if (actual.intentos() < maximoIntentos) {
                return Decision.permitido();
            }
            return new Decision(true, reintentarEnSegundos(ahora, actual.fin()));
        }
    }

    public void registrarFallo(String ip, String usuario) {
        synchronized (combinaciones) {
            String clave = clave(ip, usuario);
            Instant ahora = reloj.instant();
            Ventana actual = combinaciones.get(clave);
            if (actual == null || !ahora.isBefore(actual.fin())) {
                combinaciones.put(clave, new Ventana(1, ahora.plus(ventana)));
                return;
            }
            combinaciones.put(clave, new Ventana(actual.intentos() + 1, actual.fin()));
        }
    }

    public void registrarExito(String ip, String usuario) {
        synchronized (combinaciones) {
            combinaciones.remove(clave(ip, usuario));
        }
    }

    public void limpiar() {
        synchronized (combinaciones) {
            combinaciones.clear();
        }
    }

    private String clave(String ip, String usuario) {
        return normalizar(ip) + "|" + normalizar(usuario);
    }

    private String normalizar(String valor) {
        return valor == null ? "desconocido" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private long reintentarEnSegundos(Instant ahora, Instant fin) {
        Duration restante = Duration.between(ahora, fin);
        long segundos = restante.toSeconds();
        if (restante.compareTo(Duration.ofSeconds(segundos)) > 0) {
            segundos++;
        }
        return Math.max(1, segundos);
    }

    private record Ventana(int intentos, Instant fin) {
    }

    public record Decision(boolean bloqueado, long reintentarEnSegundos) {

        public static Decision permitido() {
            return new Decision(false, 0);
        }
    }
}
