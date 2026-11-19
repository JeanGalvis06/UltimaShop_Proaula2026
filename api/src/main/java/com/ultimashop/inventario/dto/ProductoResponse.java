package com.ultimashop.inventario.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ultimashop.inventario.domain.Producto;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal costo,
        BigDecimal precio,
        int stockMinimo,
        int stockActual,
        boolean activo,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductoResponse desde(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCosto(),
                producto.getPrecio(),
                producto.getStockMinimo(),
                producto.getStockActual(),
                producto.isActivo(),
                producto.getCreatedAt(),
                producto.getUpdatedAt()
        );
    }
}
