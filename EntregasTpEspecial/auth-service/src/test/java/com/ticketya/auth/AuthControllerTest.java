package com.ticketya.auth;

import com.ticketya.auth.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Registro, login con emision de JWT y alta de usuarios restringida al rol ADMIN
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String SECRETO = "secreto-de-prueba-de-al-menos-treinta-y-dos-caracteres";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepo;

    @BeforeEach
    void limpiar() {
        usuarioRepo.deleteAll();
    }

    @Test
    void registroCreaUnClienteYNoGuardaLaContrasenaEnClaro() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("Ana@Mail.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String hash = usuarioRepo.findByEmail("ana@mail.com").orElseThrow().getPasswordHash();
        assertNotEquals("secreta123", hash);
    }

    @Test
    void registroConEmailRepetidoDevuelve409() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro("ana@mail.com")))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro("ANA@mail.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void registroConContrasenaCortaDevuelve400() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana@mail.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginCorrectoDevuelveUnJwtConElIdYElRol() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro("ana@mail.com")))
                .andExpect(status().isCreated());

        MvcResult resultado = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@mail.com\",\"password\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
        Long id = usuarioRepo.findByEmail("ana@mail.com").orElseThrow().getId();
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertEquals(String.valueOf(id), claims.getSubject());
        assertEquals("CLIENTE", claims.get("rol", String.class));
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro("ana@mail.com")))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@mail.com\",\"password\":\"otra-clave\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@mail.com\",\"password\":\"secreta123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloUnAdministradorPuedeDarDeAltaUsuariosConRol() throws Exception {
        String organizador = "{\"nombre\":\"Productora\",\"email\":\"p@mail.com\",\"password\":\"secreta123\",\"rol\":\"ORGANIZADOR\"}";

        mvc.perform(post("/auth/usuarios").contentType(MediaType.APPLICATION_JSON).content(organizador))
                .andExpect(status().isForbidden());
        mvc.perform(post("/auth/usuarios").header("X-Rol", "CLIENTE")
                        .contentType(MediaType.APPLICATION_JSON).content(organizador))
                .andExpect(status().isForbidden());
        mvc.perform(post("/auth/usuarios").header("X-Rol", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(organizador))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ORGANIZADOR"));

        mvc.perform(get("/auth/usuarios").header("X-Rol", "ORGANIZADOR")).andExpect(status().isForbidden());
        mvc.perform(get("/auth/usuarios").header("X-Rol", "ADMIN")).andExpect(status().isOk());
    }

    private static String registro(String email) {
        return "{\"nombre\":\"Ana Perez\",\"email\":\"" + email + "\",\"password\":\"secreta123\"}";
    }
}
