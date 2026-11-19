package com.ultimashop.servicios.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean activo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Servicio() {
    }

    public static Servicio nuevo(String nombre, String descripcion, BigDecimal precio) {
        Servicio servicio = new Servicio();
        servicio.nombre = nombre;
        servicio.descripcion = descripcion;
        servicio.precio = precio;
        servicio.activo = true;
        return servicio;
    }

    public Long getId() {
        return id;
    }

    /** Código legible derivado del id; no se persiste para no duplicar información. */
    public String getCodigo() {
        return "SERV-" + String.format(Locale.ROOT, "%06d", id);
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void actualizarDatos(String nombre, String descripcion, BigDecimal precio) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
    }

    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }
}
