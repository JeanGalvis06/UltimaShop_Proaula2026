package com.ultimashop.ventas.domain;

import java.math.BigDecimal;

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
@Table(name = "venta_detalles")
public class VentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItemVenta tipo;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    protected VentaDetalle() {
    }

    public static VentaDetalle nuevo(Venta venta, TipoItemVenta tipo, Long referenciaId, String nombre,
                                     String codigo, int cantidad, BigDecimal precioUnitario) {
        VentaDetalle detalle = new VentaDetalle();
        detalle.venta = venta;
        detalle.tipo = tipo;
        detalle.referenciaId = referenciaId;
        detalle.nombre = nombre;
        detalle.codigo = codigo;
        detalle.cantidad = cantidad;
        detalle.precioUnitario = precioUnitario;
        detalle.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        return detalle;
    }

    public Long getId() {
        return id;
    }

    public Venta getVenta() {
        return venta;
    }

    public TipoItemVenta getTipo() {
        return tipo;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
