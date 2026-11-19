package com.ultimashop.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.ultimashop.seguridad.config.LoginRateLimitFilter;
import com.ultimashop.seguridad.config.RestAccessDeniedHandler;
import com.ultimashop.seguridad.config.RestAuthenticationEntryPoint;
import com.ultimashop.seguridad.config.SecurityConfig;
import com.ultimashop.seguridad.service.LimiteIntentosService;
import com.ultimashop.seguridad.web.ClienteIpResolver;

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        LoginRateLimitFilter.class, LimiteIntentosService.class, ClienteIpResolver.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paginaDeModulosMuestraLosCincoModulosDisponibles() throws Exception {
        mockMvc.perform(get("/modulos"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Ultima Shop")))
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(containsString("Ventas")))
                .andExpect(content().string(containsString("Inventario")))
                .andExpect(content().string(containsString("Servicios")))
                .andExpect(content().string(containsString("Movimientos")))
                .andExpect(content().string(containsString("Disponible")))
                .andExpect(content().string(not(containsString("Pendiente"))));
    }
}
