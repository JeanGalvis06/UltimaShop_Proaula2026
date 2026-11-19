package com.ultimashop.seguridad.service;

import java.util.List;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import com.ultimashop.seguridad.domain.Usuario;

@Service
public class SesionUsuarioService {

    private final SessionRegistry sessionRegistry;

    public SesionUsuarioService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void invalidarSesiones(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        List<Object> principales = sessionRegistry.getAllPrincipals();
        for (Object principal : principales) {
            if (principal instanceof Usuario registrado
                    && registrado.getId() != null
                    && registrado.getId().equals(usuario.getId())) {
                expirar(registrado);
            } else if (principal instanceof Usuario registrado
                    && registrado.getUsername() != null
                    && registrado.getUsername().equals(usuario.getUsername())) {
                expirar(registrado);
            }
        }
        // También intenta por la instancia actual, por si es la que quedó registrada.
        expirar(usuario);
    }

    private void expirar(Object principal) {
        for (SessionInformation sesion : sessionRegistry.getAllSessions(principal, false)) {
            sesion.expireNow();
        }
    }
}
