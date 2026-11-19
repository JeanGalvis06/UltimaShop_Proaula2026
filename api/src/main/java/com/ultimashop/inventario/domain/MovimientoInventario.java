package com.ultimashop.inventario.domain;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(name = "cantidad_anterior", nullable = false)
    private int cantidadAnterior;

    @Column(nullable = false)
    private int cambio;

    @Column(name = "cantidad_resultante", nullable = false)
    private int cantidadResultante;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 100)
    private String usuarioNombre;

    protected MovimientoInventario() {
    }

    public static MovimientoInventario registrar(Producto producto, TipoMovimiento tipo,
                                                   int cantidadAnterior, int cambio,
                                                   int cantidadResultante, String motivo) {
        return registrarConAuditoria(producto, tipo, cantidadAnterior, cambio,
                cantidadResultante, motivo, null, null, null);
    }

    public static MovimientoInventario registrarConAuditoria(
            Producto producto, TipoMovimiento tipo, int cantidadAnterior, int cambio,
            int cantidadResultante, String motivo, Long ventaId,
            Long usuarioId, String usuarioNombre) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.producto = producto;
        movimiento.tipo = tipo;
        movimiento.cantidadAnterior = cantidadAnterior;
        movimiento.cambio = cambio;
        movimiento.cantidadResultante = cantidadResultante;
        movimiento.motivo = motivo;
        movimiento.ventaId = ventaId;
        movimiento.usuarioId = usuarioId;
        movimiento.usuarioNombre = usuarioNombre;
        return movimiento;
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public int getCantidadAnterior() {
        return cantidadAnterior;
    }

    public int getCambio() {
        return cambio;
    }

    public int getCantidadResultante() {
        return cantidadResultante;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }
}
