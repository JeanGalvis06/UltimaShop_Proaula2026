package com.ultimashop.movimientos.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ultimashop.inventario.dto.MovimientoResponse;

public record PaginaMovimientos(
        List<MovimientoResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static PaginaMovimientos desde(Page<MovimientoResponse> pagina) {
        return new PaginaMovimientos(
                pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
