package com.ultimashop.inventario.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    @Column(name = "stock_actual", nullable = false)
    private int stockActual;

    @Column(nullable = false)
    private boolean activo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Producto() {
    }

    public static Producto nuevo(String codigo, String nombre, String descripcion,
                                 BigDecimal costo, BigDecimal precio, int stockMinimo) {
        Producto producto = new Producto();
        producto.codigo = codigo;
        producto.nombre = nombre;
        producto.descripcion = descripcion;
        producto.costo = costo;
        producto.precio = precio;
        producto.stockMinimo = stockMinimo;
        producto.stockActual = 0;
        producto.activo = true;
        return producto;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public int getStockActual() {
        return stockActual;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void actualizarDatos(String codigo, String nombre, String descripcion, BigDecimal costo,
                                BigDecimal precio, int stockMinimo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.costo = costo;
        this.precio = precio;
        this.stockMinimo = stockMinimo;
    }

    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }

    /**
     * Las existencias solo se modifican desde el servicio, junto con su movimiento.
     */
    public void fijarStock(int cantidadResultante) {
        this.stockActual = cantidadResultante;
    }
}
