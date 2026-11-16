package com.ticketya.ventas.controller;

import com.ticketya.ventas.dto.CompraDTO;
import com.ticketya.ventas.dto.PagoRequest;
import com.ticketya.ventas.dto.ReservaDTO;
import com.ticketya.ventas.dto.ReservaRequest;
import com.ticketya.ventas.exception.BadRequestException;
import com.ticketya.ventas.model.EstadoCompra;
import com.ticketya.ventas.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// El cliente se identifica con el header X-Usuario-Id (sin autenticacion en la entrega 1)
@Tag(name = "Reservas y pagos", description = "Compra en dos pasos: reservar lugares por unos minutos y pagar dentro de ese plazo")
@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @Operation(summary = "Reservar lugares temporalmente",
            description = "Sector NUMERADO: se indican los asientos. Sector NO_NUMERADO: se indica la cantidad. "
                    + "La reserva vence a los 10 minutos si no se paga. Maximo 4 entradas por cliente y evento. "
                    + "La base impide que dos clientes tomen el mismo asiento.")
    @ApiResponse(responseCode = "201", description = "Reserva creada (estado PENDIENTE)")
    @ApiResponse(responseCode = "400", description = "Faltan datos o el asiento no existe en el sector")
    @ApiResponse(responseCode = "404", description = "No existe el evento o el sector")
    @ApiResponse(responseCode = "409", description = "Asiento ya tomado, sin cupo, evento no a la venta o limite por cliente superado")
    @ApiResponse(responseCode = "503", description = "El servicio de catalogo no responde")
    @PostMapping
    public ResponseEntity<ReservaDTO> reservar(
            @Parameter(description = "Id del cliente (sin autenticacion en la entrega 1)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Valid @RequestBody ReservaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.reservar(usuarioId, req));
    }

    @Operation(summary = "Consultar una reserva propia")
    @ApiResponse(responseCode = "200", description = "Reserva encontrada (si vencio, se informa VENCIDA)")
    @ApiResponse(responseCode = "403", description = "La reserva no es del usuario")
    @ApiResponse(responseCode = "404", description = "No existe la reserva")
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDTO> obtener(
            @PathVariable Long id,
            @Parameter(description = "Id del cliente", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId) {
        return ResponseEntity.ok(reservaService.obtener(usuarioId, id));
    }

    @Operation(summary = "Pagar una reserva (idempotente)",
            description = "Con el pago aprobado se emiten las entradas con codigo QR. Con el pago rechazado la reserva se cancela "
                    + "y los lugares se liberan. Si el cliente reintenta con la misma Idempotency-Key (por ejemplo por un "
                    + "corte de conexion), recibe el mismo resultado sin que se cobre de nuevo.")
    @ApiResponse(responseCode = "200", description = "Pago aprobado: entradas emitidas")
    @ApiResponse(responseCode = "402", description = "Pago rechazado: los lugares se liberan")
    @ApiResponse(responseCode = "403", description = "La reserva no es del usuario")
    @ApiResponse(responseCode = "404", description = "No existe la reserva")
    @ApiResponse(responseCode = "409", description = "La reserva vencio o ya no esta pendiente, o hay otro pago en proceso")
    @PostMapping("/{id}/pago")
    public ResponseEntity<CompraDTO> pagar(
            @PathVariable Long id,
            @Parameter(description = "Id del cliente", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Clave unica del intento de pago", example = "pago-reserva-1")
            @RequestHeader("Idempotency-Key") String claveIdempotencia,
            @Valid @RequestBody PagoRequest req) {
        if (claveIdempotencia.isBlank()) {
            throw new BadRequestException("El header Idempotency-Key no puede estar vacio");
        }
        CompraDTO compra = reservaService.pagar(usuarioId, id, claveIdempotencia, req);
        HttpStatus status = compra.estado() == EstadoCompra.APROBADA ? HttpStatus.OK : HttpStatus.PAYMENT_REQUIRED;
        return ResponseEntity.status(status).body(compra);
    }

    @Operation(summary = "Cancelar una reserva pendiente y liberar sus lugares")
    @ApiResponse(responseCode = "204", description = "Reserva cancelada")
    @ApiResponse(responseCode = "403", description = "La reserva no es del usuario")
    @ApiResponse(responseCode = "404", description = "No existe la reserva")
    @ApiResponse(responseCode = "409", description = "La reserva no esta PENDIENTE")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @Parameter(description = "Id del cliente", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId) {
        reservaService.cancelar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
