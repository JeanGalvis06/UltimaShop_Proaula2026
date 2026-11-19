package com.ultimashop.ventas.service;

import org.springframework.http.HttpStatus;

import com.ultimashop.comun.ErrorComun;

public class VentaException extends ErrorComun {

    public VentaException(HttpStatus status, String mensaje) {
        super(status, mensaje);
    }

    public static VentaException noEncontrado(Long id) {
        return new VentaException(HttpStatus.NOT_FOUND,
                "No existe una venta con el identificador " + id + ".");
    }

    public static VentaException invalido(String mensaje) {
        return new VentaException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static VentaException conflicto(String mensaje) {
        return new VentaException(HttpStatus.CONFLICT, mensaje);
    }
}
