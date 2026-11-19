package com.ultimashop.seguridad.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ultimashop.seguridad.service.LimiteIntentosService;
import com.ultimashop.seguridad.service.UsuarioGestionService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class RecuperacionPasswordController {

    private final UsuarioGestionService usuarios;
    private final LimiteIntentosService limites;
    private final ClienteIpResolver ipResolver;

    public RecuperacionPasswordController(UsuarioGestionService usuarios, LimiteIntentosService limites,
                                          ClienteIpResolver ipResolver) {
        this.usuarios = usuarios;
        this.limites = limites;
        this.ipResolver = ipResolver;
    }

    @GetMapping("/recuperar")
    public String formularioSolicitud() {
        return "seguridad/recuperar";
    }

    @PostMapping("/recuperar")
    public String solicitar(@RequestParam(defaultValue = "") String correo,
                            HttpServletRequest request,
                            RedirectAttributes atributos) {
        String ip = ipResolver.obtener(request);
        String cuenta = correo == null ? "" : correo.trim().toLowerCase();
        if (limites.permitir(LimiteIntentosService.Tipo.RECUPERACION, cuenta, ip)) {
            try {
                usuarios.solicitarRecuperacion(correo, ip);
            } catch (Exception ex) {
                // Se responde siempre con el mismo mensaje para no filtrar información.
            }
        }
        atributos.addAttribute("recuperacion", "1");
        return "redirect:/login";
    }

    @GetMapping("/recuperar/restablecer")
    public String formularioRestablecer(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank() || usuarios.consultarTokenRecuperacion(token).isEmpty()) {
            model.addAttribute("tituloError", "Enlace no válido");
            model.addAttribute("mensajeError", "El enlace de recuperación no es válido, ya fue usado o venció.");
            return "seguridad/token-invalido";
        }
        model.addAttribute("token", token);
        return "seguridad/recuperar-restablecer";
    }

    @PostMapping("/recuperar/restablecer")
    public String restablecer(@RequestParam(defaultValue = "") String token,
                              @RequestParam(defaultValue = "") String password,
                              @RequestParam(defaultValue = "") String confirmarPassword,
                              Model model,
                              RedirectAttributes atributos) {
        if (usuarios.consultarTokenRecuperacion(token).isEmpty()) {
            model.addAttribute("tituloError", "Enlace no válido");
            model.addAttribute("mensajeError", "El enlace de recuperación no es válido, ya fue usado o venció.");
            return "seguridad/token-invalido";
        }
        if (!password.equals(confirmarPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "seguridad/recuperar-restablecer";
        }
        try {
            boolean actualizado = usuarios.restablecerContrasena(token, password);
            if (!actualizado) {
                model.addAttribute("tituloError", "Enlace no válido");
                model.addAttribute("mensajeError", "El enlace de recuperación no es válido, ya fue usado o venció.");
                return "seguridad/token-invalido";
            }
            atributos.addAttribute("restablecida", "1");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("token", token);
            model.addAttribute("error", ex.getMessage());
            return "seguridad/recuperar-restablecer";
        } catch (Exception ex) {
            model.addAttribute("token", token);
            model.addAttribute("error", "No fue posible actualizar la contraseña. Intente nuevamente.");
            return "seguridad/recuperar-restablecer";
        }
    }
}
