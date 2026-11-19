package com.ultimashop.ventas.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ultimashop.comun.ZonaHoraria;
import com.ultimashop.ventas.domain.Venta;
import com.ultimashop.ventas.domain.VentaDetalle;

public record VentaResponse(
        Long id,
        String claveIdempotencia,
        Instant fechaUtc,
        String fecha,
        String estado,
        BigDecimal total,
        BigDecimal totalProductos,
        BigDecimal totalServicios,
        String moneda,
        String usuarioNombre,
        List<VentaDetalleResponse> items
) {
    public static VentaResponse desde(Venta venta, List<VentaDetalle> detalles) {
        return new VentaResponse(
                venta.getId(), venta.getClaveIdempotencia(), venta.getFecha(),
                ZonaHoraria.formatear(venta.getFecha()), venta.getEstado(),
                venta.getTotal(), venta.getTotalProductos(), venta.getTotalServicios(),
                venta.getMoneda(), venta.getUsuarioNombre(),
                detalles.stream().map(VentaDetalleResponse::desde).toList());
    }
}
