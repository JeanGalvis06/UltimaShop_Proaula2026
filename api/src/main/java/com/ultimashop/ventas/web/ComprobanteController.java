package com.ultimashop.ventas.web;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultimashop.ventas.service.VentaService;

@Controller
public class ComprobanteController {
    private final JdbcTemplate jdbc;
    private final VentaService ventas;
    private final ObjectMapper mapper;
    public ComprobanteController(JdbcTemplate jdbc,VentaService ventas,ObjectMapper mapper) {this.jdbc=jdbc;this.ventas=ventas;this.mapper=mapper;}
    @GetMapping("/api/ventas/{id}/comprobante") @ResponseBody
    public ResponseEntity<?> api(@PathVariable long id) throws Exception {
        ventas.obtener(id);
        var filas=jdbc.queryForList("SELECT numero,total,moneda,detalle::text,creado_en FROM comprobantes WHERE venta_id=?",id);
        if(filas.isEmpty())return ResponseEntity.status(202).body(Map.of("estado","PENDIENTE","mensaje","El comprobante espera procesamiento del worker."));
        var fila=filas.getFirst();fila.put("detalle",mapper.readTree(fila.get("detalle").toString()));fila.put("estado","GENERADO");
        return ResponseEntity.ok(fila);
    }
    @GetMapping("/ventas/{id}/comprobante")
    public String pantalla(@PathVariable long id,Model model) {
        model.addAttribute("venta",ventas.obtener(id));
        var filas=jdbc.queryForList("SELECT numero,creado_en FROM comprobantes WHERE venta_id=?",id);
        model.addAttribute("comprobante",filas.isEmpty()?null:filas.getFirst());
        return "ventas/comprobante";
    }
}
