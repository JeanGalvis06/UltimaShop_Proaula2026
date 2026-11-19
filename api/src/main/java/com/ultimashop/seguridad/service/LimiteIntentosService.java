package com.ultimashop.seguridad.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Límites simples en memoria por cuenta e IP dentro de ventanas deslizantes. */
@Service
public class LimiteIntentosService {

    public enum Tipo {
        LOGIN, RECUPERACION, INVITACION
    }

    private final Map<String, Deque<Instant>> eventos = new ConcurrentHashMap<>();

    @Value("${ultimashop.seguridad.limite.login.max-cuenta:5}") private int loginMaxCuenta;
    @Value("${ultimashop.seguridad.limite.login.max-ip:15}") private int loginMaxIp;
    @Value("${ultimashop.seguridad.limite.login.ventana-minutos:15}") private int loginVentana;

    @Value("${ultimashop.seguridad.limite.recuperacion.max-cuenta:3}") private int recuperacionMaxCuenta;
    @Value("${ultimashop.seguridad.limite.recuperacion.max-ip:10}") private int recuperacionMaxIp;
    @Value("${ultimashop.seguridad.limite.recuperacion.ventana-minutos:30}") private int recuperacionVentana;

    @Value("${ultimashop.seguridad.limite.invitacion.max-cuenta:5}") private int invitacionMaxCuenta;
    @Value("${ultimashop.seguridad.limite.invitacion.max-ip:20}") private int invitacionMaxIp;
    @Value("${ultimashop.seguridad.limite.invitacion.ventana-minutos:60}") private int invitacionVentana;

    public boolean permitir(Tipo tipo, String cuenta, String ip) {
        Instant ahora = Instant.now();
        boolean cuentaOk = cuenta == null || cuenta.isBlank()
                || registrar(clave(tipo, "cuenta", cuenta), maxCuenta(tipo), ventana(tipo), ahora);
        boolean ipOk = ip == null || ip.isBlank()
                || registrar(clave(tipo, "ip", ip), maxIp(tipo), ventana(tipo), ahora);
        return cuentaOk && ipOk;
    }

    public boolean bloqueado(Tipo tipo, String cuenta, String ip) {
        Instant ahora = Instant.now();
        return estaLleno(clave(tipo, "cuenta", cuenta), maxCuenta(tipo), ventana(tipo), ahora)
                || estaLleno(clave(tipo, "ip", ip), maxIp(tipo), ventana(tipo), ahora);
    }

    public void reiniciarCuenta(Tipo tipo, String cuenta) {
        if (cuenta == null || cuenta.isBlank()) {
            return;
        }
        eventos.remove(clave(tipo, "cuenta", cuenta));
    }

    public void limpiar() {
        eventos.clear();
    }

    private boolean registrar(String clave, int maximo, Duration ventana, Instant ahora) {
        if (clave == null || clave.endsWith(":")) {
            return true;
        }
        Instant limite = ahora.minus(ventana);
        Deque<Instant> cola = eventos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst().isBefore(limite)) {
                cola.removeFirst();
            }
            if (cola.size() >= maximo) {
                return false;
            }
            cola.addLast(ahora);
            return true;
        }
    }

    private boolean estaLleno(String clave, int maximo, Duration ventana, Instant ahora) {
        if (clave == null || clave.endsWith(":")) {
            return false;
        }
        Instant limite = ahora.minus(ventana);
        Deque<Instant> cola = eventos.get(clave);
        if (cola == null) {
            return false;
        }
        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst().isBefore(limite)) {
                cola.removeFirst();
            }
            return cola.size() >= maximo;
        }
    }

    private String clave(Tipo tipo, String alcance, String valor) {
        return tipo.name() + ":" + alcance + ":" + valor.toLowerCase().trim();
    }

    private int maxCuenta(Tipo tipo) {
        return switch (tipo) {
            case LOGIN -> Math.max(1, loginMaxCuenta);
            case RECUPERACION -> Math.max(1, recuperacionMaxCuenta);
            case INVITACION -> Math.max(1, invitacionMaxCuenta);
        };
    }

    private int maxIp(Tipo tipo) {
        return switch (tipo) {
            case LOGIN -> Math.max(1, loginMaxIp);
            case RECUPERACION -> Math.max(1, recuperacionMaxIp);
            case INVITACION -> Math.max(1, invitacionMaxIp);
        };
    }

    private Duration ventana(Tipo tipo) {
        return switch (tipo) {
            case LOGIN -> Duration.ofMinutes(Math.max(1, loginVentana));
            case RECUPERACION -> Duration.ofMinutes(Math.max(1, recuperacionVentana));
            case INVITACION -> Duration.ofMinutes(Math.max(1, invitacionVentana));
        };
    }
}
