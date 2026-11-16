package com.ticketya.auth.service;

import com.ticketya.auth.dto.*;
import com.ticketya.auth.exception.ConflictException;
import com.ticketya.auth.exception.NoAutorizadoException;
import com.ticketya.auth.model.Rol;
import com.ticketya.auth.model.Usuario;
import com.ticketya.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock reloj;

    // Registro publico: siempre como CLIENTE (nadie puede auto-asignarse otro rol)
    @Transactional
    public UsuarioDTO registrar(RegistroRequest req) {
        return crear(req.nombre(), req.email(), req.password(), Rol.CLIENTE);
    }

    // Alta por un administrador, con el rol que elija
    @Transactional
    public UsuarioDTO alta(AltaUsuarioRequest req) {
        return crear(req.nombre(), req.email(), req.password(), req.rol());
    }

    @Transactional(readOnly = true)
    public TokenDTO login(LoginRequest req) {
        // Mismo mensaje si no existe el email o si la contrasena es incorrecta: no se revela cual de los dos fallo
        Usuario usuario = usuarioRepo.findByEmail(normalizar(req.email()))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new NoAutorizadoException("Email o contrasena incorrectos"));
        return new TokenDTO(jwtService.generar(usuario), "Bearer", jwtService.getExpiracionSegundos(),
                usuario.getId(), usuario.getNombre(), usuario.getRol());
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar() {
        return usuarioRepo.findAll().stream().map(AuthService::aDTO).toList();
    }

    private UsuarioDTO crear(String nombre, String email, String password, Rol rol) {
        String emailNormalizado = normalizar(email);
        if (usuarioRepo.existsByEmail(emailNormalizado)) {
            throw new ConflictException("Ya existe un usuario con ese email");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuario.setFechaAlta(LocalDateTime.now(reloj));
        return aDTO(usuarioRepo.saveAndFlush(usuario));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UsuarioDTO aDTO(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.getFechaAlta());
    }
}
