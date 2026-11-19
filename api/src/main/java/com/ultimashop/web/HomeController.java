package com.ultimashop.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/modulos")
    public String home(Model model) {
        model.addAttribute("modulos", List.of(
                new ModuloResumen("Dashboard", "Disponible", "/dashboard"),
                new ModuloResumen("Ventas", "Disponible", "/ventas"),
                new ModuloResumen("Inventario", "Disponible", "/inventario"),
                new ModuloResumen("Servicios", "Disponible", "/servicios"),
                new ModuloResumen("Movimientos", "Disponible", "/movimientos")
        ));
        return "index";
    }

    public static class ModuloResumen {
        private final String nombre;
        private final String estado;
        private final String url;

        public ModuloResumen(String nombre, String estado, String url) {
            this.nombre = nombre;
            this.estado = estado;
            this.url = url;
        }

        public String getNombre() {
            return nombre;
        }

        public String getEstado() {
            return estado;
        }

        public String getUrl() {
            return url;
        }
    }
}
