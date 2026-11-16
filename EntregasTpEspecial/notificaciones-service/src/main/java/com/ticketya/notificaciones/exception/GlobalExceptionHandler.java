package com.ticketya.notificaciones.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorDTO> headerFaltante(MissingRequestHeaderException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el header obligatorio: " + ex.getHeaderName());
    }

    private ResponseEntity<ErrorDTO> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ErrorDTO(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje));
    }
}
