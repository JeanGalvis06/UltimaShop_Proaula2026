package com.ultimashop.seguridad.correo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BrevoCorreoClient {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoCorreoClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final long connectTimeoutMs;

    @Value("${ultimashop.correo.enabled:false}")
    private boolean enabled;

    @Value("${ultimashop.correo.brevo.api-url:https://api.brevo.com/v3/smtp/email}")
    private String apiUrl;

    @Value("${ultimashop.correo.brevo.api-key:}")
    private String apiKey;

    @Value("${ultimashop.correo.from.email:}")
    private String fromEmail;

    @Value("${ultimashop.correo.from.name:Ultima Shop}")
    private String fromName;

    @Value("${ultimashop.correo.timeout.read-ms:5000}")
    private long readTimeoutMs;

    public BrevoCorreoClient(ObjectMapper objectMapper,
                             @Value("${ultimashop.correo.timeout.connect-ms:3000}") long connectTimeoutMsConfig) {
        this.objectMapper = objectMapper;
        this.connectTimeoutMs = Math.max(1000, connectTimeoutMsConfig);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.connectTimeoutMs))
                .build();
    }

    public boolean enviar(MensajeCorreo mensaje) {
        if (!enabled) {
            LOG.info("Correo no enviado: MAIL_ENABLED=false. Asunto no registrado con datos sensibles.");
            return false;
        }
        if (apiKey == null || apiKey.isBlank() || fromEmail == null || fromEmail.isBlank()) {
            LOG.warn("Correo no enviado: configuración de Brevo incompleta.");
            return false;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("email", fromEmail, "name", fromName == null ? "Ultima Shop" : fromName),
                    "to", List.of(Map.of(
                            "email", mensaje.destinatarioEmail(),
                            "name", mensaje.destinatarioNombre() == null ? "" : mensaje.destinatarioNombre())),
                    "subject", mensaje.asunto(),
                    "htmlContent", mensaje.html(),
                    "textContent", mensaje.texto()
            );
            String cuerpo = objectMapper.writeValueAsString(payload);
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(connectTimeoutMs + Math.max(1000, readTimeoutMs)))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();
            HttpResponse<String> respuesta = httpClient.send(solicitud, HttpResponse.BodyHandlers.ofString());
            int estado = respuesta.statusCode();
            if (estado >= 200 && estado < 300) {
                LOG.info("Solicitud de correo aceptada por Brevo.");
                return true;
            }
            LOG.warn("Brevo rechazó la solicitud de correo con estado {}.", estado);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.warn("Envío de correo interrumpido.");
            return false;
        } catch (Exception ex) {
            LOG.warn("No fue posible enviar el correo mediante Brevo.");
            return false;
        }
    }

    public boolean estaHabilitado() {
        return enabled && apiKey != null && !apiKey.isBlank() && fromEmail != null && !fromEmail.isBlank();
    }
}
