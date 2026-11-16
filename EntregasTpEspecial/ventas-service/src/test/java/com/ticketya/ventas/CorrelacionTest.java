package com.ticketya.ventas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Trazabilidad: el servicio respeta el X-Correlation-Id que manda el gateway y lo devuelve; si no viene, genera uno
@AutoConfigureMockMvc
class CorrelacionTest extends VentasTestBase {

    @Autowired MockMvc mvc;

    @Test
    void devuelveElMismoIdDeCorrelacionQueRecibe() throws Exception {
        mvc.perform(get("/devoluciones").header("X-Rol", "ADMIN").header("X-Correlation-Id", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "abc-123"));
    }

    @Test
    void siNoVieneIdDeCorrelacionGeneraUno() throws Exception {
        MvcResult r = mvc.perform(get("/devoluciones").header("X-Rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andReturn();
        assertDoesNotThrow(() -> UUID.fromString(r.getResponse().getHeader("X-Correlation-Id")));
    }
}
