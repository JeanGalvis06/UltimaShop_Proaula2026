package com.ultimashop.ventas.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VentaForm {

    @NotBlank(message = "Falta la clave de idempotencia. Recargue la pantalla e intente de nuevo.")
    private String claveIdempotencia;

    @Valid
    private List<ItemForm> items = new ArrayList<>(List.of(new ItemForm()));

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
    }

    public List<ItemForm> getItems() {
        return items;
    }

    public void setItems(List<ItemForm> items) {
        this.items = items;
    }

    public static class ItemForm {

        @NotBlank(message = "Seleccione producto o servicio.")
        private String tipo;

        @NotNull(message = "Seleccione el producto o servicio.")
        private Long referenciaId;

        @NotNull(message = "Indique la cantidad.")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero.")
        private Integer cantidad;

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public Long getReferenciaId() {
            return referenciaId;
        }

        public void setReferenciaId(Long referenciaId) {
            this.referenciaId = referenciaId;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }
    }
}
