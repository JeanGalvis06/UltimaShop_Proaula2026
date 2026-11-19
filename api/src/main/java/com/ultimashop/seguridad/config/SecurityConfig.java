package com.ultimashop.seguridad.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RestAuthenticationEntryPoint entryPointApi;
    private final RestAccessDeniedHandler deniedApi;
    private final LoginRateLimitFilter loginRateLimitFilter;

    public SecurityConfig(RestAuthenticationEntryPoint entryPointApi, RestAccessDeniedHandler deniedApi,
                          LoginRateLimitFilter loginRateLimitFilter) {
        this.entryPointApi = entryPointApi;
        this.deniedApi = deniedApi;
        this.loginRateLimitFilter = loginRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        var api = AntPathRequestMatcher.antMatcher("/api/**");

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/actuator/health", "/error").permitAll()
                .requestMatchers("/login", "/recuperar", "/recuperar/restablecer", "/invitacion").permitAll()

                .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/*", "/api/productos/*/movimientos").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers("/api/productos/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/servicios", "/api/servicios/*").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers("/api/servicios/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/movimientos", "/api/movimientos/**").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers(HttpMethod.GET, "/api/dashboard").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers("/api/ventas/**").hasAnyRole("ADMIN", "OPERADOR")

                .requestMatchers(HttpMethod.GET, "/inventario", "/inventario/productos/*/movimientos").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers("/inventario/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/servicios").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers("/servicios/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/movimientos", "/movimientos/**").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers("/ventas/**").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers("/", "/dashboard", "/modulos").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers("/perfil/**").authenticated()

                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(entryPointApi, api)
                .defaultAccessDeniedHandlerFor(deniedApi, api)
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        request -> !api.matches(request))
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(100)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?logout")
                .sessionRegistry(sessionRegistry)
            )
            .securityContext(context -> context
                .securityContextRepository(new HttpSessionSecurityContextRepository())
            )
            .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public static HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
