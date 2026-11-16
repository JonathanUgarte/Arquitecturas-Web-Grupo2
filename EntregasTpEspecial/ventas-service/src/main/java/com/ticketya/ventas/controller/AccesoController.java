package com.ticketya.ventas.controller;

import com.ticketya.ventas.dto.AccesoDTO;
import com.ticketya.ventas.dto.AccesoRequest;
import com.ticketya.ventas.service.AccesoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Control de acceso", description = "Validacion del QR en el ingreso al evento")
@RestController
@RequestMapping("/accesos")
@RequiredArgsConstructor
public class AccesoController {

    private final AccesoService accesoService;

    @Operation(summary = "Validar el QR de una entrada en el ingreso (personal de acceso)",
            description = "Verifica que la entrada exista, sea de ese evento y no se haya usado, y la marca como USADA. "
                    + "Un QR no puede ingresar dos veces, aunque se escanee en dos puertas al mismo tiempo. "
                    + "Cada escaneo (permitido o rechazado) queda en la bitacora de accesos.")
    @ApiResponse(responseCode = "200", description = "Ingreso permitido: la entrada quedo marcada como utilizada")
    @ApiResponse(responseCode = "403", description = "No es personal de acceso ni administrador")
    @ApiResponse(responseCode = "404", description = "No existe una entrada con ese codigo")
    @ApiResponse(responseCode = "409", description = "Entrada ya utilizada, de otro evento o no vigente (devuelta / en reintegro)")
    @PostMapping
    public ResponseEntity<AccesoDTO> validar(
            @Parameter(description = "Id del personal de acceso (lo completa el gateway a partir del JWT)", example = "5")
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @Parameter(description = "Rol (lo completa el gateway a partir del JWT)", example = "ACCESO")
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @Valid @RequestBody AccesoRequest req) {
        Seguridad.exigirRol(rol, Seguridad.ACCESO, Seguridad.ADMIN);
        return ResponseEntity.ok(accesoService.validarIngreso(req.eventoId(), req.codigoQr(), usuarioId));
    }
}
