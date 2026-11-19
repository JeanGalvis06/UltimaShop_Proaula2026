package com.ultimashop.seguridad.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ultimashop.seguridad.domain.PropositoToken;
import com.ultimashop.seguridad.domain.TokenAccesoUsuario;
import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.TokenAccesoUsuarioRepository;

@Service
public class TokenAccesoService {

    public record TokenGenerado(String valorPlano, TokenAccesoUsuario entidad) {
    }

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final TokenAccesoUsuarioRepository tokens;

    @Value("${ultimashop.seguridad.token.invitacion-horas:24}")
    private long invitacionHoras;

    @Value("${ultimashop.seguridad.token.recuperacion-minutos:30}")
    private long recuperacionMinutos;

    public TokenAccesoService(TokenAccesoUsuarioRepository tokens) {
        this.tokens = tokens;
    }

    @Transactional
    public TokenGenerado emitir(Usuario usuario, PropositoToken proposito, String ip) {
        Instant ahora = Instant.now();
        tokens.invalidarActivos(usuario.getId(), proposito, ahora);
        String valorPlano = generarValor();
        String hash = hash(valorPlano);
        Instant expira = ahora.plus(duracion(proposito));
        TokenAccesoUsuario entidad = new TokenAccesoUsuario(usuario, proposito, hash, expira, ahora, ip);
        return new TokenGenerado(valorPlano, tokens.save(entidad));
    }

    @Transactional(readOnly = true)
    public Optional<TokenAccesoUsuario> consultarValido(String valorPlano) {
        if (valorPlano == null || valorPlano.isBlank()) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(hash(valorPlano))
                .filter(token -> token.estaVigente(Instant.now()));
    }

    @Transactional
    public Optional<TokenAccesoUsuario> consumir(String valorPlano) {
        if (valorPlano == null || valorPlano.isBlank()) {
            return Optional.empty();
        }
        Instant ahora = Instant.now();
        Optional<TokenAccesoUsuario> token = tokens.findByTokenHashForUpdate(hash(valorPlano));
        if (token.isEmpty() || !token.get().estaVigente(ahora)) {
            return Optional.empty();
        }
        token.get().consumir(ahora);
        return token;
    }

    public String hash(String valorPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(valorPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular el hash del token.", ex);
        }
    }

    private Duration duracion(PropositoToken proposito) {
        return proposito == PropositoToken.INVITACION
                ? Duration.ofHours(Math.max(1, invitacionHoras))
                : Duration.ofMinutes(Math.max(1, recuperacionMinutos));
    }

    private String generarValor() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
