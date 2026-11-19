package com.ultimashop.seguridad.correo;

public record MensajeCorreo(
        String destinatarioEmail,
        String destinatarioNombre,
        String asunto,
        String html,
        String texto
) {
}
