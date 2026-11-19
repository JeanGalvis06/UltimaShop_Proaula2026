package com.ultimashop.dashboard.domain;

import java.math.BigDecimal;
import java.util.List;

import com.ultimashop.inventario.dto.MovimientoResponse;
import com.ultimashop.inventario.dto.ProductoResponse;
import com.ultimashop.ventas.dto.VentaResponse;

public record ResumenDashboard(
        String zonaHoraria,
        long ventasHoy,
        BigDecimal ingresosHoy,
        BigDecimal ingresosProductos,
        BigDecimal ingresosServicios,
        List<ProductoResponse> productosStockBajo,
        List<MovimientoResponse> ultimosMovimientos,
        List<VentaResponse> ventasRecientes
) {
}
