package com.ultimashop.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ultimashop.dashboard.domain.ResumenDashboard;
import com.ultimashop.inventario.service.InventarioService;
import com.ultimashop.movimientos.service.MovimientoConsultaService;
import com.ultimashop.ventas.service.VentaService;

@Service
@ConditionalOnProperty(name="ultimashop.dashboard.fuente",havingValue="worker")
public class ResumenDashboardWorkerService implements ProveedorResumenDashboard {
    private final JdbcTemplate jdbc;
    private final InventarioService inventario;
    private final MovimientoConsultaService movimientos;
    private final VentaService ventas;
    public ResumenDashboardWorkerService(JdbcTemplate jdbc,InventarioService inventario,MovimientoConsultaService movimientos,VentaService ventas) {
        this.jdbc=jdbc;this.inventario=inventario;this.movimientos=movimientos;this.ventas=ventas;
    }
    @Override @Transactional(readOnly=true)
    public ResumenDashboard obtener() {
        var filas=jdbc.queryForList("SELECT ventas,total,total_productos,total_servicios FROM resumen_ventas_diario WHERE dia=?",LocalDate.now(ZoneId.of("America/Bogota")));
        long cantidad=0;BigDecimal total=BigDecimal.ZERO,productos=BigDecimal.ZERO,servicios=BigDecimal.ZERO;
        if(!filas.isEmpty()) {
            var fila=filas.getFirst();cantidad=((Number)fila.get("ventas")).longValue();total=(BigDecimal)fila.get("total");
            productos=(BigDecimal)fila.get("total_productos");servicios=(BigDecimal)fila.get("total_servicios");
        }
        return new ResumenDashboard("America/Bogota",cantidad,total,productos,servicios,inventario.listar(null,true,true),movimientos.ultimos(5),ventas.listarDelDia().stream().limit(5).toList());
    }
}
