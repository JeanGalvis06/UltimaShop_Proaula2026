package com.ultimashop.seguridad.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String recuperacion,
                        @RequestParam(required = false) String restablecida,
                        @RequestParam(required = false) String invitacion,
                        Model model) {
        model.addAttribute("fallo", error != null);
        model.addAttribute("sesionCerrada", logout != null);
        model.addAttribute("recuperacionSolicitada", recuperacion != null);
        model.addAttribute("mensajeRecuperacion",
                "Si el correo está registrado, recibirá instrucciones para restablecer su contraseña.");
        model.addAttribute("passwordActualizada", restablecida != null);
        model.addAttribute("invitacionCompletada", invitacion != null);
        return "login";
    }
}
