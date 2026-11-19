package com.ultimashop.dashboard.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ultimashop.dashboard.domain.ResumenDashboard;
import com.ultimashop.dashboard.service.ProveedorResumenDashboard;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final ProveedorResumenDashboard proveedor;

    public DashboardApiController(ProveedorResumenDashboard proveedor) {
        this.proveedor = proveedor;
    }

    @GetMapping
    public ResumenDashboard ver() {
        return proveedor.obtener();
    }
}
