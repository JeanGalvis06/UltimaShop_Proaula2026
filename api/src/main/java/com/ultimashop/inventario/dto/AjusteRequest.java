package com.ultimashop.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AjusteRequest(
        @NotNull(message = "La cantidad final es obligatoria")
        @Min(value = 0, message = "La cantidad final no puede ser negativa")
        Integer cantidadFinal,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
        String motivo
) {
}
