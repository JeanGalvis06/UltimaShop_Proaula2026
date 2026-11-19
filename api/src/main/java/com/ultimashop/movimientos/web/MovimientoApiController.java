package com.ultimashop.movimientos.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ultimashop.inventario.domain.TipoMovimiento;
import com.ultimashop.movimientos.dto.PaginaMovimientos;
import com.ultimashop.movimientos.service.MovimientoConsultaService;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoApiController {

    private final MovimientoConsultaService consulta;

    public MovimientoApiController(MovimientoConsultaService consulta) {
        this.consulta = consulta;
    }

    @GetMapping
    public PaginaMovimientos listar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return PaginaMovimientos.desde(consulta.consultar(productoId, tipo, desde, hasta, page, size));
    }
}
