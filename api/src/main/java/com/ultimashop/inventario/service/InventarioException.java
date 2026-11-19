package com.ultimashop.inventario.service;

import org.springframework.http.HttpStatus;

import com.ultimashop.comun.ErrorComun;

public class InventarioException extends ErrorComun {

    public InventarioException(HttpStatus status, String mensaje) {
        super(status, mensaje);
    }

    public static InventarioException noEncontrado(Long id) {
        return new InventarioException(HttpStatus.NOT_FOUND,
                "No existe un producto con el identificador " + id + ".");
    }

    public static InventarioException conflicto(String mensaje) {
        return new InventarioException(HttpStatus.CONFLICT, mensaje);
    }

    public static InventarioException invalido(String mensaje) {
        return new InventarioException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
