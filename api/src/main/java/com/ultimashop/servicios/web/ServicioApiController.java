package com.ultimashop.servicios.web;

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

import com.ultimashop.servicios.dto.ServicioRequest;
import com.ultimashop.servicios.dto.ServicioResponse;
import com.ultimashop.servicios.service.ServicioService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/servicios")
public class ServicioApiController {

    private final ServicioService servicio;

    public ServicioApiController(ServicioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest solicitud) {
        ServicioResponse respuesta = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/servicios/" + respuesta.id())).body(respuesta);
    }

    @GetMapping
    public List<ServicioResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return servicio.listar(q, activo);
    }

    @GetMapping("/{id}")
    public ServicioResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/{id}")
    public ServicioResponse editar(@PathVariable Long id,
                                   @Valid @RequestBody ServicioRequest solicitud) {
        return servicio.editar(id, solicitud);
    }

    @PatchMapping("/{id}/activo")
    public ServicioResponse cambiarActivo(@PathVariable Long id,
                                          @Valid @RequestBody CambioActivoServicioRequest solicitud) {
        return servicio.cambiarActivo(id, solicitud.activo());
    }

    public record CambioActivoServicioRequest(boolean activo) {
    }
}
