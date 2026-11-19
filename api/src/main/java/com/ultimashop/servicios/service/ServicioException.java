package com.ultimashop.servicios.service;

import org.springframework.http.HttpStatus;

import com.ultimashop.comun.ErrorComun;

public class ServicioException extends ErrorComun {

    public ServicioException(HttpStatus status, String mensaje) {
        super(status, mensaje);
    }

    public static ServicioException noEncontrado(Long id) {
        return new ServicioException(HttpStatus.NOT_FOUND,
                "No existe un servicio con el identificador " + id + ".");
    }

    public static ServicioException invalido(String mensaje) {
        return new ServicioException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
