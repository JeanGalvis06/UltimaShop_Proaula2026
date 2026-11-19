package com.ultimashop.ventas.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.ultimashop.comun.Auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave_idempotencia", nullable = false, length = 80, unique = true)
    private String claveIdempotencia;

    @Column(name = "solicitud_hash", nullable = false, length = 64)
    private String solicitudHash;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "total_productos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalProductos;

    @Column(name = "total_servicios", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalServicios;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 100)
    private String usuarioNombre;

    protected Venta() {
    }

    public static Venta nueva(String claveIdempotencia, String solicitudHash,
                              BigDecimal totalProductos, BigDecimal totalServicios, Auditoria auditoria) {
        Venta venta = new Venta();
        venta.claveIdempotencia = claveIdempotencia;
        venta.solicitudHash = solicitudHash;
        venta.totalProductos = totalProductos;
        venta.totalServicios = totalServicios;
        venta.total = totalProductos.add(totalServicios);
        venta.estado = "CONFIRMADA";
        venta.moneda = "COP";
        venta.usuarioId = auditoria.usuarioId();
        venta.usuarioNombre = auditoria.nombreParaAuditoria();
        return venta;
    }

    public Long getId() {
        return id;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public String getSolicitudHash() {
        return solicitudHash;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getEstado() {
        return estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getTotalProductos() {
        return totalProductos;
    }

    public BigDecimal getTotalServicios() {
        return totalServicios;
    }

    public String getMoneda() {
        return moneda;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }
}
