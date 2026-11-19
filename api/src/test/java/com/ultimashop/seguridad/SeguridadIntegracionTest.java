package com.ultimashop.seguridad;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.UsuarioRepository;
import com.ultimashop.seguridad.service.InicializadorUsuario;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "APP_ADMIN_USERNAME=admin_prueba",
        "APP_ADMIN_PASSWORD=ClaveSegura123",
        "APP_ADMIN_NAME=Admin Seguridad"
})
@AutoConfigureMockMvc
class SeguridadIntegracionTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void conexionBaseDePruebas(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder codificador;
    @Autowired private InicializadorUsuario inicializador;

    @Test
    void usuarioInicialSeCreaDesdeVariablesConHashBcrypt() {
        Usuario usuario = usuarios.findByUsername("admin_prueba").orElseThrow();

        assertEquals("Admin Seguridad", usuario.getNombre());
        assertEquals(Usuario.ROL_ADMIN, usuario.getRol());
        assertTrue(usuario.isActivo());
        assertTrue(usuario.isCorreoVerificado());
        assertFalse("ClaveSegura123".equals(usuario.getPassword()));
        assertTrue(codificador.matches("ClaveSegura123", usuario.getPassword()));
    }

    @Test
    void reinicioNoRestableceLaContrasenaYaGuardada() {
        Usuario anterior = usuarios.findByUsername("admin_prueba").orElseThrow();
        String hashOriginal = anterior.getPassword();

        try {
            ReflectionTestUtils.setField(inicializador, "username", "admin_prueba");
            ReflectionTestUtils.setField(inicializador, "password", "OtraClave456");
            ReflectionTestUtils.setField(inicializador, "nombre", "Nombre Cambiado");
            inicializador.crearUsuarioInicial();

            Usuario posterior = usuarios.findByUsername("admin_prueba").orElseThrow();
            assertEquals(hashOriginal, posterior.getPassword());
            assertEquals("Admin Seguridad", posterior.getNombre());
            assertTrue(codificador.matches("ClaveSegura123", posterior.getPassword()));
            assertFalse(codificador.matches("OtraClave456", posterior.getPassword()));
        } finally {
            ReflectionTestUtils.setField(inicializador, "password", "ClaveSegura123");
            ReflectionTestUtils.setField(inicializador, "nombre", "Admin Seguridad");
        }
    }

    @Test
    void loginConFormularioCreaSesionYCerrarSesionRequierePost() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin_prueba")
                        .param("password", "ClaveSegura123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        assertNotNull(sesion);

        mockMvc.perform(get("/dashboard").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dashboard")));

        mockMvc.perform(post("/logout").session(sesion))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/logout").session(sesion).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(sesion.isInvalid());
    }

    @Test
    void contrasenaIncorrectaDevuelveErrorEnLogin() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin_prueba")
                        .param("password", "ClaveIncorrecta123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void urlPublicasDeRecuperacionEInvitacionRespondenSinSesion() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Olvidé mi contraseña")));
        mockMvc.perform(get("/recuperar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Olvidé mi contraseña")));
        mockMvc.perform(get("/recuperar/restablecer").param("token", "invalido"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enlace no válido")));
        mockMvc.perform(get("/invitacion").param("token", "invalido"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invitación no válida")));
    }

    @Test
    void apiSinSesionResponde401JsonYPantallasRedirigenAlLogin() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value(containsString("Debe iniciar sesión")));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void permisoInsuficienteEnApiResponde403Json() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").value(containsString("No tiene permisos")));
    }

    @Test
    @WithMockUser(username = "operador", roles = "OPERADOR")
    void operadorPuedeConsultarEiniciarVentaPeroNoGestionar() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/servicios"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/movimientos"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/ventas/nueva"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/productos").with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"OP1\",\"nombre\":\"No permitido\",\"costo\":1,\"precio\":2,\"stockMinimo\":0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEsPublicoYOcultaDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}
