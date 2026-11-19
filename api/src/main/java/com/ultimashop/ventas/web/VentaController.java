package com.ultimashop.ventas.web;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ultimashop.inventario.service.InventarioService;
import com.ultimashop.servicios.service.ServicioService;
import com.ultimashop.ventas.dto.VentaItemRequest;
import com.ultimashop.ventas.dto.VentaRequest;
import com.ultimashop.ventas.dto.VentaResponse;
import com.ultimashop.ventas.service.VentaException;
import com.ultimashop.ventas.service.VentaService;

import jakarta.validation.Valid;

@Controller
public class VentaController {

    private final VentaService servicioVentas;
    private final InventarioService servicioInventario;
    private final ServicioService servicioServicios;

    public VentaController(VentaService servicioVentas, InventarioService servicioInventario,
                           ServicioService servicioServicios) {
        this.servicioVentas = servicioVentas;
        this.servicioInventario = servicioInventario;
        this.servicioServicios = servicioServicios;
    }

    @GetMapping("/ventas")
    public String historial(Model model) {
        model.addAttribute("ventas", servicioVentas.listar());
        return "ventas/list";
    }

    @GetMapping("/ventas/nueva")
    public String nueva(Model model) {
        VentaForm formulario = new VentaForm();
        formulario.setClaveIdempotencia(UUID.randomUUID().toString());
        model.addAttribute("ventaForm", formulario);
        cargarCatalogo(model);
        return "ventas/form";
    }

    @PostMapping("/ventas/nueva")
    public String confirmar(@Valid @ModelAttribute("ventaForm") VentaForm formulario,
                            BindingResult resultado, Model model,
                            RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            cargarCatalogo(model);
            return "ventas/form";
        }
        List<VentaItemRequest> items = formulario.getItems().stream()
                .map(item -> new VentaItemRequest(item.getTipo(), item.getReferenciaId(), item.getCantidad()))
                .toList();
        try {
            VentaResponse venta = servicioVentas.confirmar(
                    new VentaRequest(formulario.getClaveIdempotencia(), items));
            atributos.addFlashAttribute("mensaje",
                    "Venta " + venta.id() + " confirmada. Stock, movimiento y evento quedaron registrados.");
            return "redirect:/ventas/" + venta.id();
        } catch (VentaException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarCatalogo(model);
            return "ventas/form";
        }
    }

    @GetMapping("/ventas/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes atributos) {
        try {
            model.addAttribute("venta", servicioVentas.obtener(id));
            return "ventas/detalle";
        } catch (VentaException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
            return "redirect:/ventas";
        }
    }

    private void cargarCatalogo(Model model) {
        model.addAttribute("productos", servicioInventario.listarEntidades(null, true, false));
        model.addAttribute("servicios", servicioServicios.listarEntidades(null, true));
    }
}
