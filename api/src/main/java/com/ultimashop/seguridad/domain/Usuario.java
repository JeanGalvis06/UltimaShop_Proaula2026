package com.ultimashop.seguridad.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class Usuario implements UserDetails {

    public static final String ROL_ADMIN = "ROLE_ADMIN";
    public static final String ROL_OPERADOR = "ROLE_OPERADOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 254)
    private String email;

    @Column(name = "email_normalizado", length = 254)
    private String emailNormalizado;

    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado;

    @Column(nullable = false, length = 40)
    private String rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "sesiones_invalidas_desde")
    private Instant sesionesInvalidasDesde;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Usuario() {
    }

    public Usuario(String username, String passwordHash, String nombre, String rol, boolean activo) {
        this(username, passwordHash, nombre, null, null, true, rol, activo, null);
    }

    public Usuario(String username, String passwordHash, String nombre, String email, String emailNormalizado,
                   boolean correoVerificado, String rol, boolean activo, Instant sesionesInvalidasDesde) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.email = email;
        this.emailNormalizado = emailNormalizado;
        this.correoVerificado = correoVerificado;
        this.rol = rolValido(rol);
        this.activo = activo;
        this.sesionesInvalidasDesde = sesionesInvalidasDesde;
    }

    public static Usuario nuevo(String username, String passwordHash, String nombre, String rol, boolean activo) {
        String rolFinal = rolValido(rol);
        boolean verificado = ROL_ADMIN.equals(rolFinal) || ROL_OPERADOR.equals(rolFinal);
        return new Usuario(username, passwordHash, nombre, null, null, verificado, rolFinal, activo, null);
    }

    public static Usuario invitado(String username, String passwordHash, String nombre, String email,
                                   String emailNormalizado, String rol) {
        return new Usuario(username, passwordHash, nombre, email, emailNormalizado, false, rol, false, null);
    }

    public static String normalizarCorreo(String correo) {
        if (correo == null) {
            return null;
        }
        String valor = correo.trim().toLowerCase(Locale.ROOT);
        return valor.isEmpty() ? null : valor;
    }

    public static String rolValido(String rol) {
        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException("Rol no permitido.");
        }
        String valor = rol.trim().toUpperCase(Locale.ROOT);
        if (valor.startsWith("ROLE_")) {
            valor = valor.substring(5);
        }
        if ("ADMIN".equals(valor)) {
            return ROL_ADMIN;
        }
        if ("OPERADOR".equals(valor)) {
            return ROL_OPERADOR;
        }
        throw new IllegalArgumentException("Rol no permitido.");
    }

    public void actualizarPerfil(String nombre) {
        this.nombre = nombre;
    }

    public void asignarCorreo(String correo) {
        this.email = correo;
        this.emailNormalizado = normalizarCorreo(correo);
    }

    public void cambiarPassword(String passwordHash, Instant sesionesInvalidasDesde) {
        this.passwordHash = passwordHash;
        this.sesionesInvalidasDesde = sesionesInvalidasDesde;
    }

    public void activarInvitacion(String passwordHash, String nombre) {
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.activo = true;
        this.correoVerificado = true;
        this.sesionesInvalidasDesde = Instant.now();
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public boolean esAdmin() {
        return ROL_ADMIN.equals(rol);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailNormalizado() {
        return emailNormalizado;
    }

    public boolean isCorreoVerificado() {
        return correoVerificado;
    }

    public String getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getSesionesInvalidasDesde() {
        return sesionesInvalidasDesde;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo && correoVerificado;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Usuario other)) {
            return false;
        }
        if (id != null && other.id != null) {
            return id.equals(other.id);
        }
        return username != null && username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (username != null ? username.hashCode() : 0);
    }
}
