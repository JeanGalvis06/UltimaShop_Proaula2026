package com.ultimashop.dashboard.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ultimashop.dashboard.domain.ResumenDashboard;
import com.ultimashop.inventario.service.InventarioService;
import com.ultimashop.movimientos.service.MovimientoConsultaService;
import com.ultimashop.ventas.dto.VentaResponse;
import com.ultimashop.ventas.service.VentaService;

@Service
@ConditionalOnProperty(name = "ultimashop.dashboard.fuente", havingValue = "tablas", matchIfMissing = true)
public class ResumenDashboardTablasService implements ProveedorResumenDashboard {

    private final VentaService ventas;
    private final InventarioService inventario;
    private final MovimientoConsultaService movimientos;

    public ResumenDashboardTablasService(VentaService ventas, InventarioService inventario,
                                         MovimientoConsultaService movimientos) {
        this.ventas = ventas;
        this.inventario = inventario;
        this.movimientos = movimientos;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenDashboard obtener() {
        List<VentaResponse> ventasHoy = ventas.listarDelDia();
        BigDecimal ingresosProductos = ventasHoy.stream()
                .map(VentaResponse::totalProductos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ingresosServicios = ventasHoy.stream()
                .map(VentaResponse::totalServicios)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumenDashboard(
                "America/Bogota",
                ventasHoy.size(),
                ingresosProductos.add(ingresosServicios),
                ingresosProductos,
                ingresosServicios,
                inventario.listar(null, true, true),
                movimientos.ultimos(5),
                ventasHoy.stream().limit(5).toList());
    }
}
