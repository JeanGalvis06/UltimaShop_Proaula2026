package com.ultimashop.inventario.dto;

import jakarta.validation.constraints.NotNull;

public record CambioActivoRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
