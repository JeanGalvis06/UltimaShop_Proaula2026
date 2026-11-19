package com.ultimashop.comun;

import org.springframework.http.HttpStatus;

/**
 * Error de reglas de negocio que puede convertirse en una respuesta JSON comprensible.
 */
public abstract class ErrorComun extends RuntimeException {

    private final HttpStatus status;

    protected ErrorComun(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
