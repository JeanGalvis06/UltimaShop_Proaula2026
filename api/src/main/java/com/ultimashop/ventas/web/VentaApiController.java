package com.ultimashop.ventas.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ultimashop.ventas.dto.VentaRequest;
import com.ultimashop.ventas.dto.VentaResponse;
import com.ultimashop.ventas.service.VentaService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/ventas")
public class VentaApiController {

    private final VentaService servicio;

    public VentaApiController(VentaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<VentaResponse> confirmar(@Valid @RequestBody VentaRequest solicitud) {
        return ResponseEntity.status(201).body(servicio.confirmar(solicitud));
    }

    @GetMapping
    public List<VentaResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }
}
