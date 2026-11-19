package com.ultimashop.seguridad.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/** Resuelve la IP del cliente. Solo confía en X-Forwarded-For si se habilita explícitamente. */
@Component
public class ClienteIpResolver {

    @Value("${ultimashop.seguridad.confia-proxy-inversa:false}")
    private boolean confiaProxyInversa;

    public String obtener(HttpServletRequest request) {
        if (request == null) {
            return "desconocida";
        }
        if (confiaProxyInversa) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int coma = forwarded.indexOf(',');
                String valor = (coma > 0 ? forwarded.substring(0, coma) : forwarded).trim();
                if (!valor.isEmpty()) {
                    return recortar(valor);
                }
            }
        }
        String remota = request.getRemoteAddr();
        return remota == null || remota.isBlank() ? "desconocida" : recortar(remota);
    }

    private String recortar(String valor) {
        return valor.length() > 45 ? valor.substring(0, 45) : valor;
    }
}
