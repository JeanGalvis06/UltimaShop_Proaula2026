package com.ultimashop.seguridad.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UsuarioDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Credenciales inválidas.");
        }
        String valor = username.trim();
        Optional<Usuario> usuario = usuarios.findByUsername(valor);
        if (usuario.isEmpty()) {
            String correo = Usuario.normalizarCorreo(valor);
            if (correo != null) {
                usuario = usuarios.findByEmailNormalizado(correo);
            }
        }
        return usuario
                .filter(Usuario::isEnabled)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));
    }
}
