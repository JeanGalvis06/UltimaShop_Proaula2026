package com.ultimashop.movimientos.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ultimashop.inventario.domain.TipoMovimiento;
import com.ultimashop.inventario.dto.MovimientoResponse;
import com.ultimashop.inventario.service.InventarioService;
import com.ultimashop.movimientos.service.MovimientoConsultaService;

@Controller
@RequestMapping("/movimientos")
public class MovimientoController {

    private final MovimientoConsultaService consulta;
    private final InventarioService inventario;

    public MovimientoController(MovimientoConsultaService consulta, InventarioService inventario) {
        this.consulta = consulta;
        this.inventario = inventario;
    }

    @GetMapping
    public String listar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {
        TipoMovimiento filtroTipo = parsearTipo(tipo);
        LocalDate fechaDesde = parsearFecha(desde);
        LocalDate fechaHasta = parsearFecha(hasta);
        Page<MovimientoResponse> pagina = consulta.consultar(
                productoId, filtroTipo, fechaDesde, fechaHasta, page, size);

        model.addAttribute("movimientos", pagina);
        model.addAttribute("productos", inventario.listarEntidades(null, null, null));
        model.addAttribute("productoId", productoId);
        model.addAttribute("tipo", tipo == null ? "" : tipo);
        model.addAttribute("desde", desde == null ? "" : desde);
        model.addAttribute("hasta", hasta == null ? "" : hasta);
        model.addAttribute("page", pagina.getNumber());
        model.addAttribute("size", pagina.getSize());
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("totalElements", pagina.getTotalElements());
        return "movimientos/list";
    }

    private TipoMovimiento parsearTipo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return TipoMovimiento.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
