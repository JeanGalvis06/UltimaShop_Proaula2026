package com.ultimashop.seguridad.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.UsuarioRepository;
import com.ultimashop.seguridad.service.UsuarioGestionService;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioGestionService gestion;
    private final UsuarioRepository usuarios;

    public PerfilController(UsuarioGestionService gestion, UsuarioRepository usuarios) {
        this.gestion = gestion;
        this.usuarios = usuarios;
    }

    @GetMapping
    public String ver(@AuthenticationPrincipal Usuario principal, Model model) {
        Usuario usuario = usuarios.findById(principal.getId()).orElse(principal);
        model.addAttribute("usuario", usuario);
        return "seguridad/perfil";
    }

    @PostMapping("/nombre")
    public String actualizarNombre(@AuthenticationPrincipal Usuario principal,
                                   @RequestParam(defaultValue = "") String nombre,
                                   RedirectAttributes atributos) {
        try {
            gestion.actualizarPerfil(principal.getId(), nombre);
            atributos.addFlashAttribute("mensaje", "Nombre actualizado correctamente.");
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("errorNombre", ex.getMessage());
        } catch (Exception ex) {
            atributos.addFlashAttribute("errorNombre", "No fue posible actualizar el nombre.");
        }
        return "redirect:/perfil";
    }

    @PostMapping("/contrasena")
    public String cambiarContrasena(@AuthenticationPrincipal Usuario principal,
                                    @RequestParam(defaultValue = "") String actual,
                                    @RequestParam(defaultValue = "") String nueva,
                                    @RequestParam(defaultValue = "") String confirmar,
                                    RedirectAttributes atributos) {
        if (!nueva.equals(confirmar)) {
            atributos.addFlashAttribute("errorPassword", "La nueva contraseña y su confirmación no coinciden.");
            return "redirect:/perfil";
        }
        try {
            gestion.cambiarContrasenaPerfil(principal.getId(), actual, nueva);
            atributos.addFlashAttribute("mensaje", "Contraseña actualizada. Las sesiones anteriores fueron cerradas.");
        } catch (IllegalArgumentException ex) {
            atributos.addFlashAttribute("errorPassword", ex.getMessage());
        } catch (Exception ex) {
            atributos.addFlashAttribute("errorPassword", "No fue posible actualizar la contraseña.");
        }
        return "redirect:/perfil";
    }
}
