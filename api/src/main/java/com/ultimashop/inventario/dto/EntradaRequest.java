package com.ultimashop.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EntradaRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad de la entrada debe ser mayor que cero")
        Integer cantidad,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
        String motivo
) {
}
