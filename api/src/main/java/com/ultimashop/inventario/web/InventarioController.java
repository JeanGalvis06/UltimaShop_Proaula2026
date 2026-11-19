package com.ultimashop.inventario.web;

import java.math.BigDecimal;
import java.util.List;

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

import com.ultimashop.inventario.domain.Producto;
import com.ultimashop.inventario.dto.AjusteRequest;
import com.ultimashop.inventario.dto.EntradaRequest;
import com.ultimashop.inventario.dto.ProductoCreateRequest;
import com.ultimashop.inventario.dto.ProductoUpdateRequest;
import com.ultimashop.inventario.service.InventarioException;
import com.ultimashop.inventario.service.InventarioService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final InventarioService servicio;

    public InventarioController(InventarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String activo,
                         @RequestParam(name = "stockBajo", required = false) String stockBajo,
                         Model model) {
        Boolean filtroActivo = null;
        if (activo != null && !activo.isBlank()) {
            filtroActivo = Boolean.valueOf(activo);
        }
        boolean filtroStockBajo = "true".equalsIgnoreCase(stockBajo);

        model.addAttribute("productos", servicio.listarEntidades(q, filtroActivo, filtroStockBajo));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("activo", filtroActivo);
        model.addAttribute("stockBajo", filtroStockBajo);
        agregarResumen(model);
        return "inventario/list";
    }

    @GetMapping("/productos/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("productoForm", new ProductoForm());
        model.addAttribute("modo", "crear");
        return "inventario/form";
    }

    @PostMapping("/productos/nuevo")
    public String crear(@Valid @ModelAttribute("productoForm") ProductoForm formulario,
                        BindingResult resultado, Model model, RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            model.addAttribute("modo", "crear");
            return "inventario/form";
        }
        try {
            servicio.crear(new ProductoCreateRequest(
                    formulario.getCodigo(), formulario.getNombre(), formulario.getDescripcion(),
                    formulario.getCosto(), formulario.getPrecio(), formulario.getStockMinimo()));
            atributos.addFlashAttribute("mensaje", "Producto creado con existencias en cero.");
        } catch (InventarioException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("modo", "crear");
            return "inventario/form";
        }
        return "redirect:/inventario";
    }

    @GetMapping("/productos/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes atributos) {
        Producto producto;
        try {
            producto = servicio.buscarEntidad(id);
        } catch (InventarioException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
            return "redirect:/inventario";
        }
        ProductoForm formulario = new ProductoForm();
        formulario.setCodigo(producto.getCodigo());
        formulario.setNombre(producto.getNombre());
        formulario.setDescripcion(producto.getDescripcion());
        formulario.setCosto(producto.getCosto());
        formulario.setPrecio(producto.getPrecio());
        formulario.setStockMinimo(producto.getStockMinimo());

        model.addAttribute("productoForm", formulario);
        model.addAttribute("modo", "editar");
        model.addAttribute("productoId", id);
        model.addAttribute("stockActual", producto.getStockActual());
        return "inventario/form";
    }

    @PostMapping("/productos/{id}/editar")
    public String guardarEdicion(@PathVariable Long id,
                                 @Valid @ModelAttribute("productoForm") ProductoForm formulario,
                                 BindingResult resultado, Model model,
                                 RedirectAttributes atributos) {
        if (resultado.hasErrors()) {
            prepararFormularioEdicion(id, model);
            return "inventario/form";
        }
        try {
            servicio.editar(id, new ProductoUpdateRequest(
                    formulario.getCodigo(), formulario.getNombre(), formulario.getDescripcion(),
                    formulario.getCosto(), formulario.getPrecio(), formulario.getStockMinimo()));
            atributos.addFlashAttribute("mensaje", "Producto actualizado. Las existencias no se modificaron.");
        } catch (InventarioException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormularioEdicion(id, model);
            return "inventario/form";
        }
        return "redirect:/inventario";
    }

    @PostMapping("/productos/{id}/activo")
    public String cambiarActivo(@PathVariable Long id,
                                @RequestParam boolean activo,
                                RedirectAttributes atributos) {
        try {
            servicio.cambiarActivo(id, activo);
            atributos.addFlashAttribute("mensaje",
                    activo ? "Producto activado." : "Producto desactivado. No se borraron sus datos.");
        } catch (InventarioException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/inventario";
    }

    @PostMapping("/productos/{id}/entradas")
    public String entrada(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String cantidad,
                          @RequestParam(defaultValue = "") String motivo,
                          RedirectAttributes atributos) {
        try {
            if (cantidad.isBlank() || motivo.isBlank()) {
                throw InventarioException.invalido("Indique la cantidad y el motivo de la entrada.");
            }
            int valor = entero(cantidad, "La cantidad debe ser un número entero.");
            servicio.registrarEntrada(id, new EntradaRequest(valor, motivo));
            atributos.addFlashAttribute("mensaje", "Entrada registrada y movimiento guardado.");
        } catch (InventarioException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/inventario";
    }

    @PostMapping("/productos/{id}/ajustes")
    public String ajuste(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String cantidadFinal,
                         @RequestParam(defaultValue = "") String motivo,
                         RedirectAttributes atributos) {
        try {
            if (cantidadFinal.isBlank() || motivo.isBlank()) {
                throw InventarioException.invalido("Indique la cantidad final y el motivo del ajuste.");
            }
            int valor = entero(cantidadFinal, "La cantidad final debe ser un número entero.");
            servicio.registrarAjuste(id, new AjusteRequest(valor, motivo));
            atributos.addFlashAttribute("mensaje", "Ajuste registrado y movimiento guardado.");
        } catch (InventarioException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/inventario";
    }

    @GetMapping("/productos/{id}/movimientos")
    public String movimientos(@PathVariable Long id, Model model, RedirectAttributes atributos) {
        try {
            model.addAttribute("producto", servicio.obtener(id));
            model.addAttribute("movimientos", servicio.movimientos(id));
            return "inventario/movimientos";
        } catch (InventarioException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
            return "redirect:/inventario";
        }
    }

    /** Contadores derivados de los productos reales del catálogo, no de la vista filtrada. */
    private void agregarResumen(Model model) {
        List<Producto> todos = servicio.listarEntidades(null, null, null);
        model.addAttribute("totalProductos", todos.size());
        model.addAttribute("productosActivos", todos.stream().filter(Producto::isActivo).count());
        model.addAttribute("unidadesTotales", todos.stream().mapToInt(Producto::getStockActual).sum());
        model.addAttribute("stockPorRevisar", todos.stream()
                .filter(p -> p.getStockActual() <= p.getStockMinimo())
                .count());
        model.addAttribute("valorInventario", todos.stream()
                .map(p -> p.getCosto().multiply(BigDecimal.valueOf(p.getStockActual())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void prepararFormularioEdicion(Long id, Model model) {
        model.addAttribute("modo", "editar");
        model.addAttribute("productoId", id);
        model.addAttribute("stockActual", servicio.buscarEntidad(id).getStockActual());
    }

    private int entero(String texto, String mensaje) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            throw InventarioException.invalido(mensaje);
        }
    }
}
