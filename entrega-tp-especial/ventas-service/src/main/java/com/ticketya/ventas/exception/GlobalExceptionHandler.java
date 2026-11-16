package com.ticketya.ventas.exception;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// Traduce las excepciones del Service a codigos HTTP
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDTO> notFound(NotFoundException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDTO> conflict(ConflictException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDTO> badRequest(BadRequestException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorDTO> forbidden(ForbiddenException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorDTO> noDisponible(ServicioNoDisponibleException ex) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    // Ultima red de seguridad: si la base rechaza por unicidad o por bloqueo, es un conflicto de concurrencia
    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<ErrorDTO> concurrencia(Exception ex) {
        return respuesta(HttpStatus.CONFLICT, "Conflicto de concurrencia: el recurso ya fue tomado o esta ocupado, reintente");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, detalle);
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
        ErrorDTO body = new ErrorDTO(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
