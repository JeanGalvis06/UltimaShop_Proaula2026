package com.ultimashop.seguridad.config;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultimashop.inventario.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {
        if (respuesta.isCommitted()) {
            return;
        }
        HttpStatus estado = HttpStatus.UNAUTHORIZED;
        ApiError error = new ApiError(Instant.now(), estado.value(), estado.getReasonPhrase(),
                "Debe iniciar sesión para usar la API.", null);
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(respuesta.getWriter(), error);
    }
}
