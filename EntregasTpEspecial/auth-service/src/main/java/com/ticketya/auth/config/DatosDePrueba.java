package com.ticketya.auth.config;

import com.ticketya.auth.dto.AltaUsuarioRequest;
import com.ticketya.auth.model.Rol;
import com.ticketya.auth.repository.UsuarioRepository;
import com.ticketya.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Carga un usuario por rol la primera vez que arranca (si la base esta vacia).
// El orden importa: los clientes quedan con id 1 y 2, igual que en la entrega 1 y en la coleccion de Postman.
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DatosDePrueba implements CommandLineRunner {

    public static final String CLAVE = "ticketya123";

    private final UsuarioRepository usuarioRepo;
    private final AuthService authService;

    @Override
    public void run(String... args) {
        if (usuarioRepo.count() > 0) {
            return;
        }
        authService.alta(new AltaUsuarioRequest("Cliente Uno", "cliente@ticketya.com", CLAVE, Rol.CLIENTE));
        authService.alta(new AltaUsuarioRequest("Cliente Dos", "cliente2@ticketya.com", CLAVE, Rol.CLIENTE));
        authService.alta(new AltaUsuarioRequest("Productora Sur", "organizador@ticketya.com", CLAVE, Rol.ORGANIZADOR));
        authService.alta(new AltaUsuarioRequest("Administrador", "admin@ticketya.com", CLAVE, Rol.ADMIN));
        authService.alta(new AltaUsuarioRequest("Personal de Acceso", "acceso@ticketya.com", CLAVE, Rol.ACCESO));
    }
}
