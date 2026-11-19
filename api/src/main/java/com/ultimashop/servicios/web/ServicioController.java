package com.ultimashop.servicios.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ultimashop.servicios.domain.Servicio;
import com.ultimashop.servicios.dto.ServicioRequest;
import com.ultimashop.servicios.service.ServicioException;
import com.ultimashop.servicios.service.ServicioService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/servicios")
public class ServicioController {

    private final ServicioService servicio;

    public ServicioController(ServicioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String activo,
                         Model model) {
        Boolean filtroActivo = activo == null || activo.isBlank() ? null : Boolean.valueOf(activo);
        model.addAttribute("servicios", servicio.listarEntidades(q, filtroActivo));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("activo", filtroActivo);
        return "servicios/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("servicioForm", new ServicioForm());
        model.addAttribute("modo", "crear");
        return "servicios/form";
    }

    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute("servicioForm") ServicioForm formulario,
                        BindingResult resultado, Model model, RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            model.addAttribute("modo", "crear");
            return "servicios/form";
        }
        try {
            servicio.crear(new ServicioRequest(formulario.getNombre(), formulario.getDescripcion(),
                    formulario.getPrecio(), true));
            atributos.addFlashAttribute("mensaje", "Servicio creado y activo.");
        } catch (ServicioException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("modo", "crear");
            return "servicios/form";
        }
        return "redirect:/servicios";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes atributos) {
        try {
            Servicio entidad = servicio.buscarEntidad(id);
            ServicioForm formulario = new ServicioForm();
            formulario.setNombre(entidad.getNombre());
            formulario.setDescripcion(entidad.getDescripcion());
            formulario.setPrecio(entidad.getPrecio());
            model.addAttribute("servicioForm", formulario);
            model.addAttribute("modo", "editar");
            model.addAttribute("servicioId", id);
            return "servicios/form";
        } catch (ServicioException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
            return "redirect:/servicios";
        }
    }

    @PostMapping("/{id}/editar")
    public String guardarEdicion(@PathVariable Long id,
                                 @Valid @ModelAttribute("servicioForm") ServicioForm formulario,
                                 BindingResult resultado, Model model,
                                 RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            model.addAttribute("modo", "editar");
            model.addAttribute("servicioId", id);
            return "servicios/form";
        }
        try {
            servicio.editar(id, new ServicioRequest(formulario.getNombre(), formulario.getDescripcion(),
                    formulario.getPrecio(), null));
            atributos.addFlashAttribute("mensaje", "Servicio actualizado. El precio aplicado a ventas futuras cambió.");
        } catch (ServicioException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("modo", "editar");
            model.addAttribute("servicioId", id);
            return "servicios/form";
        }
        return "redirect:/servicios";
    }

    @PostMapping("/{id}/activo")
    public String cambiarActivo(@PathVariable Long id, @RequestParam boolean activo,
                                RedirectAttributes atributos) {
        try {
            servicio.cambiarActivo(id, activo);
            atributos.addFlashAttribute("mensaje",
                    activo ? "Servicio activado." : "Servicio desactivado. No se borraron sus datos.");
        } catch (ServicioException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/servicios";
    }
}
