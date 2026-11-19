package com.ultimashop.dashboard.web;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@Controller
public class IntegracionController {
    private final JdbcTemplate jdbc;
    @Value("${ultimashop.dashboard.fuente:tablas}") private String fuente;
    public IntegracionController(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    private Map<String,Object> estado() {
        var estado=jdbc.queryForMap("SELECT count(*) FILTER(WHERE estado='PENDIENTE') AS pendientes,count(*) FILTER(WHERE estado='PUBLICADO') AS publicados,count(*) FILTER(WHERE estado='FALLIDO') AS fallidos FROM outbox_eventos");
        estado.put("fuenteDashboard",fuente);
        estado.put("comprobantes",jdbc.queryForObject("SELECT count(*) FROM comprobantes",Long.class));
        estado.put("ventasProyectadas",jdbc.queryForObject("SELECT count(*) FROM ventas_proyectadas",Long.class));
        estado.put("actualizadoEn",jdbc.queryForObject("SELECT max(actualizado_en) FROM resumen_ventas_diario",java.sql.Timestamp.class));
        return estado;
    }
    @GetMapping("/api/integracion/estado") @ResponseBody public Map<String,Object> api() {return estado();}
    @GetMapping("/integracion") public String pantalla(Model model) {model.addAttribute("estado",estado());return "integracion/estado";}
}
