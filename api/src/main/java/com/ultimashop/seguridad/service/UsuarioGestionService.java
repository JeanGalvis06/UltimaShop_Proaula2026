package com.ultimashop.seguridad.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ultimashop.seguridad.correo.BrevoCorreoClient;
import com.ultimashop.seguridad.correo.CorreoPlantillaService;
import com.ultimashop.seguridad.domain.PropositoToken;
import com.ultimashop.seguridad.domain.TokenAccesoUsuario;
import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.TokenAccesoUsuarioRepository;
import com.ultimashop.seguridad.repository.UsuarioRepository;

@Service
public class UsuarioGestionService {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioGestionService.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final TokenAccesoUsuarioRepository tokens;
    private final TokenAccesoService tokenAccesoService;
    private final PasswordEncoder codificador;
    private final BrevoCorreoClient correoClient;
    private final CorreoPlantillaService plantillas;
    private final SesionUsuarioService sesionUsuarioService;

    public UsuarioGestionService(UsuarioRepository usuarios, TokenAccesoUsuarioRepository tokens,
                                 TokenAccesoService tokenAccesoService, PasswordEncoder codificador,
                                 BrevoCorreoClient correoClient, CorreoPlantillaService plantillas,
                                 SesionUsuarioService sesionUsuarioService) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.tokenAccesoService = tokenAccesoService;
        this.codificador = codificador;
        this.correoClient = correoClient;
        this.plantillas = plantillas;
        this.sesionUsuarioService = sesionUsuarioService;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId() == null ? 0 : a.getId(), b.getId() == null ? 0 : b.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarios.findById(id);
    }

    @Transactional
    public ResultadoInvitacion invitar(String nombre, String correo, String rol, String ipSolicitante) {
        String nombreLimpio = validarNombre(nombre);
        String correoNormalizado = Usuario.normalizarCorreo(correo);
        if (correoNormalizado == null || !correoNormalizado.contains("@") || correoNormalizado.length() > 254) {
            throw new IllegalArgumentException("Indique un correo válido.");
        }
        String rolFinal = Usuario.rolValido(rol);
        if (usuarios.existsByEmailNormalizado(correoNormalizado)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
        }
        String username = generarUsernameUnico(correoNormalizado);
        String passwordInaccesible = codificador.encode(generarSecretoAleatorio(48));
        Usuario usuario = Usuario.invitado(username, passwordInaccesible, nombreLimpio,
                correoNormalizado, correoNormalizado, rolFinal);
        usuario = usuarios.saveAndFlush(usuario);

        TokenAccesoService.TokenGenerado token = tokenAccesoService.emitir(usuario, PropositoToken.INVITACION, ipSolicitante);
        boolean enviado = correoClient.enviar(plantillas.invitacion(
                usuario.getEmail(), usuario.getNombre(), token.valorPlano()));
        if (!enviado) {
            LOG.info("Invitación registrada sin confirmación de entrega.");
        }
        return new ResultadoInvitacion(usuario, enviado);
    }

    @Transactional
    public boolean reenviarInvitacion(Long usuarioId, String ipSolicitante) {
        Usuario usuario = usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario indicado."));
        if (usuario.isEnabled()) {
            throw new IllegalArgumentException("La cuenta ya está activa.");
        }
        if (usuario.getEmail() == null || usuario.getEmailNormalizado() == null) {
            throw new IllegalArgumentException("El usuario no tiene un correo válido para reenviar la invitación.");
        }
        TokenAccesoService.TokenGenerado token = tokenAccesoService.emitir(usuario, PropositoToken.INVITACION, ipSolicitante);
        return correoClient.enviar(plantillas.invitacion(usuario.getEmail(), usuario.getNombre(), token.valorPlano()));
    }

    @Transactional(readOnly = true)
    public Optional<TokenAccesoUsuario> consultarTokenInvitacion(String tokenPlano) {
        return tokenAccesoService.consultarValido(tokenPlano)
                .filter(t -> t.getProposito() == PropositoToken.INVITACION);
    }

    @Transactional
    public boolean aceptarInvitacion(String tokenPlano, String nombre, String password) {
        String nombreLimpio = validarNombre(nombre);
        validarPasswordNueva(password);
        Optional<TokenAccesoUsuario> consumido = tokenAccesoService.consumir(tokenPlano)
                .filter(t -> t.getProposito() == PropositoToken.INVITACION);
        if (consumido.isEmpty()) {
            return false;
        }
        Usuario usuario = usuarios.findByIdForUpdate(consumido.get().getUsuario().getId()).orElse(null);
        if (usuario == null) {
            return false;
        }
        usuario.activarInvitacion(codificador.encode(password), nombreLimpio);
        tokens.invalidarActivos(usuario.getId(), PropositoToken.INVITACION, Instant.now());
        sesionUsuarioService.invalidarSesiones(usuario);
        return true;
    }

    @Transactional
    public void solicitarRecuperacion(String correoIngresado, String ipSolicitante) {
        String correoNormalizado = Usuario.normalizarCorreo(correoIngresado);
        if (correoNormalizado == null) {
            return;
        }
        Optional<Usuario> usuarioOpt = usuarios.findByEmailNormalizado(correoNormalizado)
                .filter(Usuario::isActivo);
        if (usuarioOpt.isEmpty()) {
            return;
        }
        Usuario usuario = usuarioOpt.get();
        TokenAccesoService.TokenGenerado token = tokenAccesoService.emitir(usuario, PropositoToken.RECUPERACION, ipSolicitante);
        correoClient.enviar(plantillas.recuperacion(usuario.getEmail(), usuario.getNombre(), token.valorPlano()));
    }

    @Transactional(readOnly = true)
    public Optional<TokenAccesoUsuario> consultarTokenRecuperacion(String tokenPlano) {
        return tokenAccesoService.consultarValido(tokenPlano)
                .filter(t -> t.getProposito() == PropositoToken.RECUPERACION);
    }

    @Transactional
    public boolean restablecerContrasena(String tokenPlano, String password) {
        validarPasswordNueva(password);
        Optional<TokenAccesoUsuario> consumido = tokenAccesoService.consumir(tokenPlano)
                .filter(t -> t.getProposito() == PropositoToken.RECUPERACION);
        if (consumido.isEmpty()) {
            return false;
        }
        Usuario usuario = usuarios.findByIdForUpdate(consumido.get().getUsuario().getId()).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            return false;
        }
        Instant ahora = Instant.now();
        usuario.cambiarPassword(codificador.encode(password), ahora);
        tokens.invalidarActivos(usuario.getId(), PropositoToken.RECUPERACION, ahora);
        sesionUsuarioService.invalidarSesiones(usuario);
        if (usuario.getEmail() != null) {
            correoClient.enviar(plantillas.avisoCambioContrasena(usuario.getEmail(), usuario.getNombre()));
        }
        return true;
    }

    @Transactional
    public void actualizarPerfil(Long usuarioId, String nombre) {
        Usuario usuario = usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario."));
        usuario.actualizarPerfil(validarNombre(nombre));
    }

    @Transactional
    public void cambiarContrasenaPerfil(Long usuarioId, String actual, String nueva) {
        validarPasswordNueva(nueva);
        Usuario usuario = usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario."));
        if (actual == null || !codificador.matches(actual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        Instant ahora = Instant.now();
        usuario.cambiarPassword(codificador.encode(nueva), ahora);
        sesionUsuarioService.invalidarSesiones(usuario);
        if (usuario.getEmail() != null) {
            correoClient.enviar(plantillas.avisoCambioContrasena(usuario.getEmail(), usuario.getNombre()));
        }
    }

    @Transactional
    public void cambiarEstado(Long usuarioId, boolean activo) {
        Usuario usuario = usuarios.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario."));
        if (!activo) {
            if (usuario.esAdmin() && usuarios.countByRolAndActivoTrue(Usuario.ROL_ADMIN) <= 1) {
                throw new IllegalArgumentException("No se puede desactivar al último administrador activo.");
            }
            usuario.desactivar();
            sesionUsuarioService.invalidarSesiones(usuario);
        } else {
            // Solo se pueden activar cuentas ya verificadas; las invitaciones pendientes se activan por token.
            if (!usuario.isCorreoVerificado()) {
                throw new IllegalArgumentException("La cuenta debe completar su invitación antes de activarse.");
            }
            usuario.activar();
        }
    }

    public String validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Indique el nombre.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > 120) {
            limpio = limpio.substring(0, 120);
        }
        return limpio;
    }

    public void validarPasswordNueva(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (password.length() > 72) {
            throw new IllegalArgumentException("La contraseña no puede superar 72 caracteres.");
        }
    }

    private String generarUsernameUnico(String correoNormalizado) {
        String base = correoNormalizado;
        int arroba = base.indexOf('@');
        if (arroba > 0) {
            base = base.substring(0, arroba);
        }
        base = base.replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            base = "usuario";
        }
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        String candidato = base;
        int intento = 1;
        while (usuarios.existsByUsername(candidato)) {
            String sufijo = "-" + intento;
            int maxBase = 80 - sufijo.length();
            String recorte = base.length() > maxBase ? base.substring(0, maxBase) : base;
            candidato = recorte + sufijo;
            intento++;
        }
        return candidato;
    }

    private String generarSecretoAleatorio(int bytes) {
        byte[] valor = new byte[bytes];
        ALEATORIO.nextBytes(valor);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(valor);
    }

    public record ResultadoInvitacion(Usuario usuario, boolean enviado) {
    }
}
