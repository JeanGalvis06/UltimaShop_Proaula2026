package com.ultimashop.seguridad.correo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.util.HtmlUtils;

@Service
public class CorreoPlantillaService {

    private final String baseUrl;

    public CorreoPlantillaService(@Value("${ultimashop.correo.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "http://localhost:8080" : baseUrl.replaceAll("/+$", "");
    }

    public MensajeCorreo invitacion(String email, String nombre, String tokenPlano) {
        String enlace = enlace("/invitacion", tokenPlano);
        String nombreSeguro = HtmlUtils.htmlEscape(nombre == null ? "" : nombre);
        String asunto = "Invitación a Ultima Shop";
        String texto = "Hola " + (nombre == null ? "" : nombre) + ",\n\n"
                + "Has sido invitado a Ultima Shop. Para verificar tu correo y establecer tu contraseña, usa este enlace:\n"
                + enlace + "\n\n"
                + "Este enlace vence en 24 horas y solo puede usarse una vez.\n"
                + "Si no esperabas esta invitación, puedes ignorar este mensaje.";
        String html = plantilla(asunto,
                "<p>Hola <strong>" + nombreSeguro + "</strong>,</p>"
                        + "<p>Has sido invitado a Ultima Shop. Para verificar tu correo y establecer tu contraseña, usa el siguiente botón:</p>"
                        + boton(enlace, "Activar mi cuenta")
                        + "<p>Este enlace vence en 24 horas y solo puede usarse una vez.</p>"
                        + "<p>Si no esperabas esta invitación, puedes ignorar este mensaje.</p>"
                        + urlAlterna(enlace));
        return new MensajeCorreo(email, nombre, asunto, html, texto);
    }

    public MensajeCorreo recuperacion(String email, String nombre, String tokenPlano) {
        String enlace = enlace("/recuperar/restablecer", tokenPlano);
        String nombreSeguro = HtmlUtils.htmlEscape(nombre == null ? "usuario" : nombre);
        String asunto = "Restablece tu contraseña de Ultima Shop";
        String texto = "Hola " + (nombre == null ? "usuario" : nombre) + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña. Usa este enlace:\n"
                + enlace + "\n\n"
                + "Este enlace vence en 30 minutos y solo puede usarse una vez.\n"
                + "Si no solicitaste este cambio, puedes ignorar este mensaje.";
        String html = plantilla(asunto,
                "<p>Hola <strong>" + nombreSeguro + "</strong>,</p>"
                        + "<p>Recibimos una solicitud para restablecer tu contraseña. Usa el siguiente botón:</p>"
                        + boton(enlace, "Restablecer contraseña")
                        + "<p>Este enlace vence en 30 minutos y solo puede usarse una vez.</p>"
                        + "<p>Si no solicitaste este cambio, puedes ignorar este mensaje.</p>"
                        + urlAlterna(enlace));
        return new MensajeCorreo(email, nombre, asunto, html, texto);
    }

    public MensajeCorreo avisoCambioContrasena(String email, String nombre) {
        String nombreSeguro = HtmlUtils.htmlEscape(nombre == null ? "usuario" : nombre);
        String asunto = "Tu contraseña de Ultima Shop fue cambiada";
        String texto = "Hola " + (nombre == null ? "usuario" : nombre) + ",\n\n"
                + "Te informamos que la contraseña de tu cuenta en Ultima Shop fue cambiada.\n"
                + "Si no realizaste este cambio, contacta de inmediato a un administrador.";
        String html = plantilla(asunto,
                "<p>Hola <strong>" + nombreSeguro + "</strong>,</p>"
                        + "<p>Te informamos que la contraseña de tu cuenta en Ultima Shop fue cambiada.</p>"
                        + "<p>Si no realizaste este cambio, contacta de inmediato a un administrador.</p>");
        return new MensajeCorreo(email, nombre, asunto, html, texto);
    }

    private String enlace(String ruta, String tokenPlano) {
        return baseUrl + ruta + "?token=" + URLEncoder.encode(tokenPlano, StandardCharsets.UTF_8);
    }

    private String boton(String url, String texto) {
        String segura = HtmlUtils.htmlEscape(url);
        return "<p style=\"margin:24px 0\"><a href=\"" + segura + "\" "
                + "style=\"display:inline-block;background:#6d28d9;color:#ffffff;text-decoration:none;"
                + "padding:12px 18px;border-radius:8px;font-weight:600\">" + HtmlUtils.htmlEscape(texto) + "</a></p>";
    }

    private String urlAlterna(String url) {
        String segura = HtmlUtils.htmlEscape(url);
        return "<p style=\"font-size:13px;color:#555\">Si el botón no funciona, copia y pega este enlace en tu navegador:<br>"
                + "<span style=\"word-break:break-all\">" + segura + "</span></p>";
    }

    private String plantilla(String titulo, String contenido) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + HtmlUtils.htmlEscape(titulo) + "</title></head>"
                + "<body style=\"margin:0;padding:24px;background:#f5f3ff;font-family:Arial,sans-serif;color:#1f2937\">"
                + "<div style=\"max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e9d5ff;"
                + "border-radius:12px;padding:24px\">"
                + "<h1 style=\"margin:0 0 16px;font-size:20px;color:#5b21b6\">Ultima Shop</h1>"
                + contenido
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:24px 0\">"
                + "<p style=\"font-size:12px;color:#6b7280;margin:0\">Mensaje automático. No respondas a este correo.</p>"
                + "</div></body></html>";
    }
}
