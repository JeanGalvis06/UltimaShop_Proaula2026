package com.ultimashop.ventas.dto;

import java.math.BigDecimal;

import com.ultimashop.ventas.domain.VentaDetalle;

public record VentaDetalleResponse(
        Long id,
        String tipo,
        Long referenciaId,
        String nombre,
        String codigo,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static VentaDetalleResponse desde(VentaDetalle detalle) {
        return new VentaDetalleResponse(
                detalle.getId(), detalle.getTipo().name(), detalle.getReferenciaId(),
                detalle.getNombre(), detalle.getCodigo(), detalle.getCantidad(),
                detalle.getPrecioUnitario(), detalle.getSubtotal());
    }
}
