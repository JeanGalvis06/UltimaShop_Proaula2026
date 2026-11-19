package com.ultimashop.seguridad.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ultimashop.seguridad.service.UsuarioGestionService;

@Controller
public class InvitacionController {

    private final UsuarioGestionService usuarios;

    public InvitacionController(UsuarioGestionService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/invitacion")
    public String formulario(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank() || usuarios.consultarTokenInvitacion(token).isEmpty()) {
            model.addAttribute("tituloError", "Invitación no válida");
            model.addAttribute("mensajeError", "El enlace de invitación no es válido, ya fue usado o venció.");
            return "seguridad/token-invalido";
        }
        var tokenEntidad = usuarios.consultarTokenInvitacion(token).orElseThrow();
        model.addAttribute("token", token);
        model.addAttribute("nombre", tokenEntidad.getUsuario().getNombre());
        return "seguridad/invitacion";
    }

    @PostMapping("/invitacion")
    public String aceptar(@RequestParam(defaultValue = "") String token,
                          @RequestParam(defaultValue = "") String nombre,
                          @RequestParam(defaultValue = "") String password,
                          @RequestParam(defaultValue = "") String confirmarPassword,
                          Model model,
                          RedirectAttributes atributos) {
        if (usuarios.consultarTokenInvitacion(token).isEmpty()) {
            model.addAttribute("tituloError", "Invitación no válida");
            model.addAttribute("mensajeError", "El enlace de invitación no es válido, ya fue usado o venció.");
            return "seguridad/token-invalido";
        }
        if (!password.equals(confirmarPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("nombre", nombre);
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "seguridad/invitacion";
        }
        try {
            boolean activada = usuarios.aceptarInvitacion(token, nombre, password);
            if (!activada) {
                model.addAttribute("tituloError", "Invitación no válida");
                model.addAttribute("mensajeError", "El enlace de invitación no es válido, ya fue usado o venció.");
                return "seguridad/token-invalido";
            }
            atributos.addAttribute("invitacion", "1");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("token", token);
            model.addAttribute("nombre", nombre);
            model.addAttribute("error", ex.getMessage());
            return "seguridad/invitacion";
        } catch (Exception ex) {
            model.addAttribute("token", token);
            model.addAttribute("nombre", nombre);
            model.addAttribute("error", "No fue posible activar la cuenta. Intente nuevamente.");
            return "seguridad/invitacion";
        }
    }
}
