package com.ticketya.preferencias.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

// Traduce las excepciones a codigos HTTP (mismo formato de error que el resto de los servicios)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorDTO> forbidden(ForbiddenException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDTO> badRequest(BadRequestException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({ConflictException.class})
    public ResponseEntity<ErrorDTO> conflict(ConflictException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDTO> concurrencia(OptimisticLockingFailureException ex) {
        return respuesta(HttpStatus.CONFLICT, "El perfil se modifico al mismo tiempo desde otro lado, reintente");
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorDTO> noDisponible(ServicioNoDisponibleException ex) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorDTO> headerFaltante(MissingRequestHeaderException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el header obligatorio: " + ex.getHeaderName());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorDTO> formatoInvalido(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Parametro o cuerpo con formato invalido");
    }

    private ResponseEntity<ErrorDTO> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ErrorDTO(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje));
    }
}
