package com.ultimashop.seguridad.config;

import java.io.IOException;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ultimashop.seguridad.service.LimiteIntentosService;
import com.ultimashop.seguridad.web.ClienteIpResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final AntPathRequestMatcher LOGIN_POST =
            new AntPathRequestMatcher("/login", "POST");

    private final LimiteIntentosService limites;
    private final ClienteIpResolver ipResolver;

    public LoginRateLimitFilter(LimiteIntentosService limites, ClienteIpResolver ipResolver) {
        this.limites = limites;
        this.ipResolver = ipResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (LOGIN_POST.matches(request)) {
            String cuenta = request.getParameter("username");
            String ip = ipResolver.obtener(request);
            if (limites.bloqueado(LimiteIntentosService.Tipo.LOGIN, cuenta, ip)) {
                response.sendRedirect(request.getContextPath() + "/login?error");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
