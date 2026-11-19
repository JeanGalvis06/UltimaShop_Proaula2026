package com.ultimashop.inventario.dto;

import java.time.Instant;

import com.ultimashop.comun.ZonaHoraria;
import com.ultimashop.inventario.domain.MovimientoInventario;
import com.ultimashop.inventario.domain.TipoMovimiento;

public record MovimientoResponse(
        Long id,
        Long productoId,
        String codigoProducto,
        String nombreProducto,
        TipoMovimiento tipo,
        int cantidadAnterior,
        int cambio,
        int cantidadResultante,
        Instant fecha,
        String fechaLocal,
        String motivo,
        String usuarioNombre,
        Long ventaId
) {
    public static MovimientoResponse desde(MovimientoInventario movimiento) {
        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getProducto().getId(),
                movimiento.getProducto().getCodigo(),
                movimiento.getProducto().getNombre(),
                movimiento.getTipo(),
                movimiento.getCantidadAnterior(),
                movimiento.getCambio(),
                movimiento.getCantidadResultante(),
                movimiento.getFecha(),
                ZonaHoraria.formatear(movimiento.getFecha()),
                movimiento.getMotivo(),
                movimiento.getUsuarioNombre(),
                movimiento.getVentaId()
        );
    }
}
