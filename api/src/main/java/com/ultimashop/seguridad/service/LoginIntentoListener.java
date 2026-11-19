package com.ultimashop.seguridad.service;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class LoginIntentoListener {

    private final LimiteIntentosService limites;

    public LoginIntentoListener(LimiteIntentosService limites) {
        this.limites = limites;
    }

    @EventListener
    public void fallo(AuthenticationFailureBadCredentialsEvent evento) {
        String cuenta = evento.getAuthentication() == null ? null : evento.getAuthentication().getName();
        String ip = extraerIp(evento.getAuthentication() == null ? null : evento.getAuthentication().getDetails());
        limites.permitir(LimiteIntentosService.Tipo.LOGIN, cuenta, ip);
    }

    @EventListener
    public void exito(AuthenticationSuccessEvent evento) {
        String cuenta = evento.getAuthentication() == null ? null : evento.getAuthentication().getName();
        limites.reiniciarCuenta(LimiteIntentosService.Tipo.LOGIN, cuenta);
    }

    private String extraerIp(Object details) {
        if (details instanceof WebAuthenticationDetails web) {
            return web.getRemoteAddress();
        }
        return null;
    }
}
