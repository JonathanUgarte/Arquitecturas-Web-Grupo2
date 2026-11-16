package com.ticketya.auth.controller;

import com.ticketya.auth.dto.*;
import com.ticketya.auth.exception.ForbiddenException;
import com.ticketya.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Autenticacion", description = "Registro, login (emite el JWT) y alta de usuarios con rol")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrarse como cliente (publico)")
    @ApiResponse(responseCode = "201", description = "Usuario creado con rol CLIENTE")
    @ApiResponse(responseCode = "400", description = "Datos invalidos (email mal formado, contrasena corta)")
    @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese email")
    @PostMapping("/registro")
    public ResponseEntity<UsuarioDTO> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req));
    }

    @Operation(summary = "Iniciar sesion (publico)",
            description = "Devuelve un JWT valido por 60 minutos. Enviarlo en 'Authorization: Bearer <token>'.")
    @ApiResponse(responseCode = "200", description = "Login correcto: token emitido")
    @ApiResponse(responseCode = "401", description = "Email o contrasena incorrectos")
    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(summary = "Dar de alta un usuario con cualquier rol (administrador)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "401", description = "Sin token o token invalido (lo responde el gateway)")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese email")
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioDTO> alta(
            @Parameter(hidden = true) @RequestHeader(value = "X-Rol", required = false) String rol,
            @Valid @RequestBody AltaUsuarioRequest req) {
        exigirAdministrador(rol);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.alta(req));
    }

    @Operation(summary = "Listar usuarios (administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Usuarios registrados")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDTO>> listar(
            @Parameter(hidden = true) @RequestHeader(value = "X-Rol", required = false) String rol) {
        exigirAdministrador(rol);
        return ResponseEntity.ok(authService.listar());
    }

    // El gateway valida el JWT y reenvia el rol en X-Rol (borrando cualquier X-Rol que mande el cliente)
    private void exigirAdministrador(String rol) {
        if (!"ADMIN".equals(rol)) {
            throw new ForbiddenException("Esta accion requiere el rol ADMIN");
        }
    }
}
