package com.ultimashop.inventario.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ultimashop.comun.ErrorComun;
import com.ultimashop.dashboard.web.DashboardApiController;
import com.ultimashop.inventario.dto.ApiError;
import com.ultimashop.movimientos.web.MovimientoApiController;
import com.ultimashop.servicios.web.ServicioApiController;
import com.ultimashop.ventas.web.VentaApiController;

@RestControllerAdvice(assignableTypes = {
        ProductoApiController.class,
        ServicioApiController.class,
        VentaApiController.class,
        MovimientoApiController.class,
        DashboardApiController.class
})
public class ManejadorErroresApi {

    @ExceptionHandler(ErrorComun.class)
    public ResponseEntity<ApiError> reglasDeNegocio(ErrorComun ex) {
        return construir(ex.getStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "Revise los campos indicados.", campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> solicitudInvalida(Exception ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "La solicitud no tiene un formato válido. Revise los datos enviados.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> errorGeneral(Exception ex) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intente nuevamente.", null);
    }

    private ResponseEntity<ApiError> construir(HttpStatus estado, String mensaje,
                                               Map<String, String> campos) {
        ApiError error = new ApiError(Instant.now(), estado.value(),
                estado.getReasonPhrase(), mensaje, campos);
        return ResponseEntity.status(estado).body(error);
    }
}
