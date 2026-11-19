package com.ultimashop.seguridad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.UsuarioRepository;

/** Crea el primer usuario solo si no existe; nunca restablece una contraseña ya guardada. */
@Component
public class InicializadorUsuario {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorUsuario.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    @Value("${APP_ADMIN_USERNAME:}")
    private String username;

    @Value("${APP_ADMIN_PASSWORD:}")
    private String password;

    @Value("${APP_ADMIN_NAME:}")
    private String nombre;

    public InicializadorUsuario(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void crearUsuarioInicial() {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            LOG.warn("No se creó usuario inicial: defina APP_ADMIN_USERNAME y APP_ADMIN_PASSWORD.");
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD debe tener al menos 8 caracteres.");
        }
        if (usuarios.existsByUsername(username.trim())) {
            LOG.info("El usuario inicial ya existe; su contraseña no se modificó.");
            return;
        }
        String nombreFinal = nombre == null || nombre.isBlank() ? username.trim() : nombre.trim();
        if (nombreFinal.length() > 120) {
            nombreFinal = nombreFinal.substring(0, 120);
        }
        usuarios.saveAndFlush(Usuario.nuevo(
                username.trim(), codificador.encode(password), nombreFinal, Usuario.ROL_ADMIN, true));
        LOG.info("Usuario administrador inicial creado desde variables de entorno: {}", username.trim());
    }
}
