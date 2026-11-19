package com.ultimashop.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.ultimashop.seguridad.domain.PropositoToken;
import com.ultimashop.seguridad.domain.TokenAccesoUsuario;
import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.TokenAccesoUsuarioRepository;
import com.ultimashop.seguridad.repository.UsuarioRepository;
import com.ultimashop.seguridad.service.LimiteIntentosService;
import com.ultimashop.seguridad.service.TokenAccesoService;
import com.ultimashop.seguridad.service.UsuarioGestionService;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "APP_ADMIN_USERNAME=admin_prueba",
        "APP_ADMIN_PASSWORD=ClaveSegura123",
        "APP_ADMIN_NAME=Admin Seguridad",
        "ultimashop.correo.enabled=true",
        "ultimashop.correo.brevo.api-key=clave-simulada",
        "ultimashop.correo.from.email=prueba@example.test",
        "ultimashop.correo.from.name=Ultima Shop Prueba",
        "ultimashop.correo.base-url=https://app.ejemplo.test",
        "RECUPERACION_TOKEN_MINUTOS=30",
        "INVITACION_TOKEN_HORAS=24"
})
@AutoConfigureMockMvc
class UsuariosCorreoIntegracionTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static HttpServer servidorBrevo;
    static final List<JsonNode> SOLICITUDES = Collections.synchronizedList(new ArrayList<>());
    static volatile int estadoBrevo = 202;
    static volatile boolean fallarConexion = false;

    static {
        try {
            servidorBrevo = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidorBrevo.createContext("/v3/smtp/email", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws java.io.IOException {
                    byte[] cuerpo = exchange.getRequestBody().readAllBytes();
                    try {
                        if (fallarConexion) {
                            exchange.close();
                            return;
                        }
                        JsonNode json = new ObjectMapper().readTree(cuerpo);
                        SOLICITUDES.add(json);
                        String respuesta = estadoBrevo >= 200 && estadoBrevo < 300
                                ? "{\"messageId\":\"simulado\"}" : "{\"message\":\"rechazado\"}";
                        byte[] salida = respuesta.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(estadoBrevo, salida.length);
                        exchange.getResponseBody().write(salida);
                    } catch (Exception ex) {
                        exchange.sendResponseHeaders(500, -1);
                    } finally {
                        exchange.close();
                    }
                }
            });
            servidorBrevo.start();
        } catch (java.io.IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("ultimashop.correo.brevo.api-url", () ->
                "http://127.0.0.1:" + servidorBrevo.getAddress().getPort() + "/v3/smtp/email");
    }

    @BeforeAll
    static void inicializarServidor() {
        assertNotNull(servidorBrevo);
    }

    @AfterAll
    static void detenerServidor() {
        if (servidorBrevo != null) {
            servidorBrevo.stop(0);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TokenAccesoUsuarioRepository tokens;
    @Autowired private TokenAccesoService tokenAccesoService;
    @Autowired private UsuarioGestionService gestion;
    @Autowired private PasswordEncoder codificador;
    @Autowired private LimiteIntentosService limites;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void limpiar() {
        tokens.deleteAll();
        usuarios.deleteAll();
        usuarios.saveAndFlush(Usuario.nuevo("admin_prueba", codificador.encode("ClaveSegura123"),
                "Admin Seguridad", Usuario.ROL_ADMIN, true));
        SOLICITUDES.clear();
        estadoBrevo = 202;
        fallarConexion = false;
        limites.limpiar();
    }

    @Test
    void invitacionCreaUsuarioInactivoConCorreoYTokenSeguro() throws Exception {
        var resultado = gestion.invitar("  Maria Invitada  ", "  MARIA.EJEMPLO@EXAMPLE.COM ", "OPERADOR", "127.0.0.1");

        Usuario invitado = usuarios.findById(resultado.usuario().getId()).orElseThrow();
        assertEquals("MARIA.EJEMPLO@EXAMPLE.COM".toLowerCase(), invitado.getEmailNormalizado());
        assertEquals("Maria Invitada", invitado.getNombre());
        assertEquals(Usuario.ROL_OPERADOR, invitado.getRol());
        assertFalse(invitado.isActivo());
        assertFalse(invitado.isCorreoVerificado());
        assertTrue(invitado.getPassword().length() >= 60);

        assertEquals(1, SOLICITUDES.size());
        JsonNode correo = SOLICITUDES.get(0);
        assertEquals("prueba@example.test", correo.get("sender").get("email").asText());
        assertEquals("maria.ejemplo@example.com", correo.get("to").get(0).get("email").asText());
        String html = correo.get("htmlContent").asText();
        assertTrue(html.contains("https://app.ejemplo.test/invitacion?token="));
        assertFalse(html.contains("ClaveSegura123"));
        assertFalse(html.contains("password="));

        List<TokenAccesoUsuario> guardados = tokens.findAll();
        assertEquals(1, guardados.size());
        assertNotEquals(guardados.get(0).getTokenHash(), extraerToken(html));
        assertEquals(64, guardados.get(0).getTokenHash().length());
    }

    @Test
    void getDeInvitacionNoConsumeTokenYPostSi() throws Exception {
        var resultado = gestion.invitar("Ana", "ana@example.test", "ADMIN", "127.0.0.1");
        String token = extraerToken(SOLICITUDES.get(0).get("htmlContent").asText());

        mockMvc.perform(get("/invitacion").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Activar cuenta")));
        assertTrue(gestion.consultarTokenInvitacion(token).isPresent());

        mockMvc.perform(post("/invitacion").with(csrf())
                        .param("token", token)
                        .param("nombre", "Ana Aceptada")
                        .param("password", "ClaveNueva123")
                        .param("confirmarPassword", "ClaveNueva123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?invitacion=1"));

        Usuario usuario = usuarios.findById(resultado.usuario().getId()).orElseThrow();
        assertTrue(usuario.isActivo());
        assertTrue(usuario.isCorreoVerificado());
        assertTrue(codificador.matches("ClaveNueva123", usuario.getPassword()));
        assertTrue(gestion.consultarTokenInvitacion(token).isEmpty());

        mockMvc.perform(post("/invitacion").with(csrf())
                        .param("token", token)
                        .param("nombre", "Ana Aceptada")
                        .param("password", "ClaveNueva123")
                        .param("confirmarPassword", "ClaveNueva123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enlace no válido")));
    }

    @Test
    void tokenExpiradoYReutilizadoSonInvalidos() {
        var resultado = gestion.invitar("Luis", "luis@example.test", "OPERADOR", "127.0.0.1");
        String token = extraerToken(SOLICITUDES.get(0).get("htmlContent").asText());

        jdbc.update("update tokens_acceso_usuario set expira_en = ? where token_hash = ?",
                java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)),
                tokenAccesoService.hash(token));

        assertTrue(gestion.consultarTokenInvitacion(token).isEmpty());
        assertFalse(gestion.aceptarInvitacion(token, "Luis", "ClaveNueva123"));
        assertFalse(usuarios.findById(resultado.usuario().getId()).orElseThrow().isActivo());
    }

    @Test
    void nuevoTokenInvalidaElAnteriorDelMismoProposito() {
        var resultado = gestion.invitar("Carla", "carla@example.test", "OPERADOR", "127.0.0.1");
        String primero = extraerToken(SOLICITUDES.get(0).get("htmlContent").asText());
        gestion.reenviarInvitacion(resultado.usuario().getId(), "127.0.0.1");
        String segundo = extraerToken(SOLICITUDES.get(1).get("htmlContent").asText());

        assertFalse(segundo.equals(primero));
        assertTrue(gestion.consultarTokenInvitacion(primero).isEmpty());
        assertTrue(gestion.consultarTokenInvitacion(segundo).isPresent());
    }

    @Test
    void consumoConcurrenteDelMismoTokenSoloActivaUnaVez() throws Exception {
        gestion.invitar("Diana", "diana@example.test", "OPERADOR", "127.0.0.1");
        String token = extraerToken(SOLICITUDES.get(0).get("htmlContent").asText());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<Boolean>> futuros = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futuros.add(executor.submit(() -> {
                inicio.await();
                return gestion.aceptarInvitacion(token, "Diana", "ClaveNueva123");
            }));
        }
        inicio.countDown();
        boolean primero = futuros.get(0).get(60, TimeUnit.SECONDS);
        boolean segundo = futuros.get(1).get(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(primero ^ segundo, "Solo una petición debe consumir el token");
        assertEquals(1, tokens.count());
        assertEquals(1, tokens.findAll().stream().filter(t -> t.getConsumidoEn() != null).count());
    }

    @Test
    void recuperacionRespondeIgualParaCuentaExistenteInexistenteYRevocaSesiones() throws Exception {
        Usuario admin = usuarios.findByUsername("admin_prueba").orElseThrow();
        admin.asignarCorreo("admin@example.test");
        usuarios.saveAndFlush(admin);

        mockMvc.perform(post("/recuperar").with(csrf()).param("correo", "admin@example.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?recuperacion=1"));
        mockMvc.perform(post("/recuperar").with(csrf()).param("correo", "noexiste@example.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?recuperacion=1"));

        assertEquals(1, SOLICITUDES.size());
        String html = SOLICITUDES.get(0).get("htmlContent").asText();
        assertTrue(html.contains("https://app.ejemplo.test/recuperar/restablecer?token="));
        assertFalse(html.contains("admin@example.test</span>"));
        String token = extraerToken(html);

        MvcResult acceso = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin_prueba").param("password", "ClaveSegura123"))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession sesion = (MockHttpSession) acceso.getRequest().getSession(false);
        assertNotNull(sesion);

        mockMvc.perform(get("/recuperar/restablecer").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Restablecer contraseña")));
        assertTrue(gestion.consultarTokenRecuperacion(token).isPresent());

        mockMvc.perform(post("/recuperar/restablecer").with(csrf())
                        .param("token", token)
                        .param("password", "NuevaClave456")
                        .param("confirmarPassword", "NuevaClave456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?restablecida=1"));

        assertFalse(codificador.matches("ClaveSegura123", usuarios.findByUsername("admin_prueba").orElseThrow().getPassword()));
        assertTrue(codificador.matches("NuevaClave456", usuarios.findByUsername("admin_prueba").orElseThrow().getPassword()));
        assertTrue(gestion.consultarTokenRecuperacion(token).isEmpty());
        mockMvc.perform(get("/dashboard").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
        assertTrue(sesion.isInvalid());
        assertEquals(2, SOLICITUDES.size()); //Uno de recuperación y otro de aviso de cambio.
    }

    @Test
    void perfilCambiaNombreConCsrfYValidaContrasenaActual() throws Exception {
        Usuario admin = usuarios.findByUsername("admin_prueba").orElseThrow();
        admin.asignarCorreo("admin@example.test");
        usuarios.saveAndFlush(admin);

        MvcResult acceso = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin_prueba").param("password", "ClaveSegura123"))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession sesion = (MockHttpSession) acceso.getRequest().getSession(false);

        mockMvc.perform(get("/perfil").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mi perfil")));
        mockMvc.perform(get("/usuarios").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuarios")));
        mockMvc.perform(get("/usuarios/nuevo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invitar usuario")));

        mockMvc.perform(post("/perfil/nombre")
                        .param("nombre", "Sin sesión"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/perfil/nombre").session(sesion).with(csrf())
                        .param("nombre", "Nombre Editado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil"));
        assertEquals("Nombre Editado", usuarios.findById(admin.getId()).orElseThrow().getNombre());

        mockMvc.perform(post("/perfil/contrasena").session(sesion).with(csrf())
                        .param("actual", "Incorrecta123")
                        .param("nueva", "OtraClave789")
                        .param("confirmar", "OtraClave789"))
                .andExpect(status().is3xxRedirection());
        assertTrue(codificador.matches("ClaveSegura123", usuarios.findById(admin.getId()).orElseThrow().getPassword()));
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdministradorActivo() {
        Usuario admin = usuarios.findByUsername("admin_prueba").orElseThrow();
        assertEquals(1, usuarios.countByRolAndActivoTrue(Usuario.ROL_ADMIN));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> gestion.cambiarEstado(admin.getId(), false));
        assertTrue(error.getMessage().contains("último administrador"));
        assertTrue(usuarios.findById(admin.getId()).orElseThrow().isActivo());
    }

    @Test
    void erroresDeBrevoNoFiltranDetallesYNoAfirmanEntrega() {
        estadoBrevo = 500;
        var resultado = gestion.invitar("Elena", "elena@example.test", "ADMIN", "127.0.0.1");
        assertFalse(resultado.enviado());
        assertTrue(usuarios.findByEmailNormalizado("elena@example.test").isPresent());

        fallarConexion = true;
        var segundo = gestion.invitar("Felipe", "felipe@example.test", "OPERADOR", "127.0.0.1");
        assertFalse(segundo.enviado());
        assertFalse(SOLICITUDES.toString().contains("clave-simulada"));
    }

    @Test
    void limiteDeLoginPorCuentaSeAplica() throws Exception {
        limites.limpiar();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login").with(csrf())
                            .param("username", "admin_prueba").param("password", "Incorrecta" + i))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin_prueba").param("password", "ClaveSegura123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void rolOperadorNoPuedeInvitarYTampocoDesactivarUsuarios() throws Exception {
        Usuario operador = Usuario.nuevo("operador", codificador.encode("ClaveSegura123"),
                "Operador", Usuario.ROL_OPERADOR, true);
        usuarios.saveAndFlush(operador);

        MvcResult acceso = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "operador").param("password", "ClaveSegura123"))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession sesion = (MockHttpSession) acceso.getRequest().getSession(false);

        mockMvc.perform(get("/usuarios").session(sesion))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/usuarios/nuevo").session(sesion).with(csrf())
                        .param("nombre", "No permitido")
                        .param("correo", "no@example.test")
                        .param("rol", "ADMIN"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/productos").session(sesion).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"NO\",\"nombre\":\"No permitido\",\"costo\":1,\"precio\":2,\"stockMinimo\":0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/ventas/nueva").session(sesion))
                .andExpect(status().isOk());
    }

    @Test
    void tokenPlanoNoSeGuardaEnBase() {
        gestion.invitar("Gabriela", "gabriela@example.test", "OPERADOR", "127.0.0.1");
        String token = extraerToken(SOLICITUDES.get(0).get("htmlContent").asText());
        String sqlite = jdbc.queryForObject(
                "select token_hash from tokens_acceso_usuario where usuario_id = ?",
                String.class, usuarios.findByEmailNormalizado("gabriela@example.test").orElseThrow().getId());
        assertNotEquals(token, sqlite);
        assertEquals(tokenAccesoService.hash(token), sqlite);
        assertFalse(jdbc.queryForObject("select exists(select 1 from tokens_acceso_usuario where token_hash = ?)",
                Boolean.class, token));
    }

    private String extraerToken(String html) {
        int posicion = html.indexOf("?token=");
        assertTrue(posicion > 0);
        String resto = html.substring(posicion + 7);
        int fin = resto.indexOf('"');
        if (fin < 0) fin = resto.length();
        return java.net.URLDecoder.decode(resto.substring(0, fin), StandardCharsets.UTF_8);
    }
}
