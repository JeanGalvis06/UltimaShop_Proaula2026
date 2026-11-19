package com.ultimashop.dashboard.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ultimashop.dashboard.service.ProveedorResumenDashboard;

@Controller
public class DashboardController {

    private final ProveedorResumenDashboard proveedor;

    public DashboardController(ProveedorResumenDashboard proveedor) {
        this.proveedor = proveedor;
    }

    @GetMapping({"/", "/dashboard"})
    public String ver(Model model) {
        model.addAttribute("resumen", proveedor.obtener());
        return "dashboard/index";
    }
}
