package com.ultimashop.seguridad.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ultimashop.seguridad.service.LimiteIntentosService;
import com.ultimashop.seguridad.service.UsuarioGestionService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/usuarios")
public class UsuarioAdminController {

    private final UsuarioGestionService gestion;
    private final LimiteIntentosService limites;
    private final ClienteIpResolver ipResolver;

    public UsuarioAdminController(UsuarioGestionService gestion, LimiteIntentosService limites,
                                  ClienteIpResolver ipResolver) {
        this.gestion = gestion;
        this.limites = limites;
        this.ipResolver = ipResolver;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", gestion.listar());
        return "seguridad/usuarios-list";
    }

    @GetMapping("/nuevo")
    public String formularioInvitacion(Model model) {
        if (!model.containsAttribute("nombre")) model.addAttribute("nombre", "");
        if (!model.containsAttribute("correo")) model.addAttribute("correo", "");
        if (!model.containsAttribute("rol")) model.addAttribute("rol", "OPERADOR");
        return "seguridad/usuarios-nuevo";
    }

    @PostMapping("/nuevo")
    public String invitar(@RequestParam(defaultValue = "") String nombre,
                          @RequestParam(defaultValue = "") String correo,
                          @RequestParam(defaultValue = "") String rol,
                          HttpServletRequest request,
                          RedirectAttributes atributos) {
        String ip = ipResolver.obtener(request);
        String cuenta = correo == null ? "" : correo.trim().toLowerCase();
        if (!limites.permitir(LimiteIntentosService.Tipo.INVITACION, cuenta, ip)) {
            atributos.addFlashAttribute("error", "Se alcanzó el límite de invitaciones. Intente más tarde.");
            atributos.addFlashAttribute("nombre", nombre);
            atributos.addFlashAttribute("correo", correo);
            atributos.addFlashAttribute("rol", rol);
            return "redirect:/usuarios/nuevo";
        }
        try {
            var resultado = gestion.invitar(nombre, correo, rol, ip);
            atributos.addFlashAttribute("mensaje",
                    "Invitación creada para " + resultado.usuario().getEmail() + ". "
                            + (resultado.enviado() ? "La solicitud de envío fue aceptada." : "El correo no pudo enviarse con la configuración actual."));
            return "redirect:/usuarios";
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
            atributos.addFlashAttribute("nombre", nombre);
            atributos.addFlashAttribute("correo", correo);
            atributos.addFlashAttribute("rol", rol);
            return "redirect:/usuarios/nuevo";
        } catch (Exception ex) {
            atributos.addFlashAttribute("error", "No fue posible crear la invitación.");
            atributos.addFlashAttribute("nombre", nombre);
            atributos.addFlashAttribute("correo", correo);
            atributos.addFlashAttribute("rol", rol);
            return "redirect:/usuarios/nuevo";
        }
    }

    @PostMapping("/{id}/reenviar")
    public String reenviar(@PathVariable Long id, HttpServletRequest request, RedirectAttributes atributos) {
        String ip = ipResolver.obtener(request);
        if (!limites.permitir(LimiteIntentosService.Tipo.INVITACION, "reenviar:" + id, ip)) {
            atributos.addFlashAttribute("error", "Se alcanzó el límite de reenvíos. Intente más tarde.");
            return "redirect:/usuarios";
        }
        try {
            boolean enviado = gestion.reenviarInvitacion(id, ip);
            atributos.addFlashAttribute("mensaje",
                    enviado ? "La invitación fue reenviada." : "No fue posible enviar el correo con la configuración actual.");
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            atributos.addFlashAttribute("error", "No fue posible reenviar la invitación.");
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam boolean activo, RedirectAttributes atributos) {
        try {
            gestion.cambiarEstado(id, activo);
            atributos.addFlashAttribute("mensaje", activo ? "Usuario activado." : "Usuario desactivado.");
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            atributos.addFlashAttribute("error", "No fue posible actualizar el estado del usuario.");
        }
        return "redirect:/usuarios";
    }
}
