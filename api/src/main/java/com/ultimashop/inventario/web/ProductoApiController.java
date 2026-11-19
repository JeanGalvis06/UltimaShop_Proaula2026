package com.ultimashop.inventario.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ultimashop.inventario.dto.AjusteRequest;
import com.ultimashop.inventario.dto.CambioActivoRequest;
import com.ultimashop.inventario.dto.EntradaRequest;
import com.ultimashop.inventario.dto.MovimientoResponse;
import com.ultimashop.inventario.dto.ProductoCreateRequest;
import com.ultimashop.inventario.dto.ProductoResponse;
import com.ultimashop.inventario.dto.ProductoUpdateRequest;
import com.ultimashop.inventario.service.InventarioService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/productos")
public class ProductoApiController {

    private final InventarioService servicio;

    public ProductoApiController(InventarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoCreateRequest solicitud) {
        ProductoResponse respuesta = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/productos/" + respuesta.id())).body(respuesta);
    }

    @GetMapping
    public List<ProductoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(name = "stockBajo", required = false) Boolean stockBajo) {
        return servicio.listar(q, activo, stockBajo);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/{id}")
    public ProductoResponse editar(@PathVariable Long id,
                                   @Valid @RequestBody ProductoUpdateRequest solicitud) {
        return servicio.editar(id, solicitud);
    }

    @PatchMapping("/{id}/activo")
    public ProductoResponse cambiarActivo(@PathVariable Long id,
                                          @Valid @RequestBody CambioActivoRequest solicitud) {
        return servicio.cambiarActivo(id, solicitud.activo());
    }

    @PostMapping("/{id}/entradas")
    public ResponseEntity<MovimientoResponse> registrarEntrada(
            @PathVariable Long id, @Valid @RequestBody EntradaRequest solicitud) {
        return ResponseEntity.status(201).body(servicio.registrarEntrada(id, solicitud));
    }

    @PostMapping("/{id}/ajustes")
    public ResponseEntity<MovimientoResponse> registrarAjuste(
            @PathVariable Long id, @Valid @RequestBody AjusteRequest solicitud) {
        return ResponseEntity.status(201).body(servicio.registrarAjuste(id, solicitud));
    }

    @GetMapping("/{id}/movimientos")
    public List<MovimientoResponse> movimientos(@PathVariable Long id) {
        return servicio.movimientos(id);
    }
}
