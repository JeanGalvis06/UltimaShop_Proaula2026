package com.ultimashop.servicios.dto;

import java.math.BigDecimal;

import com.ultimashop.servicios.domain.Servicio;

public record ServicioResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        boolean activo
) {
    public static ServicioResponse desde(Servicio servicio) {
        return new ServicioResponse(
                servicio.getId(), servicio.getCodigo(), servicio.getNombre(),
                servicio.getDescripcion(), servicio.getPrecio(), servicio.isActivo());
    }
}
