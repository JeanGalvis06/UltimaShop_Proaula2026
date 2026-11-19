package com.ultimashop.seguridad.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.ultimashop.comun.Auditoria;
import com.ultimashop.comun.ProveedorAuditoria;
import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.UsuarioRepository;

@Component
public class AuditoriaProveedor implements ProveedorAuditoria {

    private final UsuarioRepository usuarios;

    public AuditoriaProveedor(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Auditoria obtener() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion.getPrincipal() == null) {
            return Auditoria.ANONIMA;
        }
        Object principal = autenticacion.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return new Auditoria(usuario.getId(), usuario.getNombre());
        }
        String nombre = autenticacion.getName();
        if (nombre == null || nombre.isBlank() || "anonymousUser".equals(nombre)) {
            return Auditoria.ANONIMA;
        }
        return usuarios.findByUsername(nombre)
                .or(() -> {
                    String correo = Usuario.normalizarCorreo(nombre);
                    return correo == null ? java.util.Optional.empty() : usuarios.findByEmailNormalizado(correo);
                })
                .map(usuario -> new Auditoria(usuario.getId(), usuario.getNombre()))
                .orElseGet(() -> new Auditoria(null, nombre));
    }
}
