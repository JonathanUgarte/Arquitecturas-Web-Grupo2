package com.ticketya.ventas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Segunda barrera de autorizacion: aunque un pedido llegara al servicio sin pasar por el gateway,
// cada endpoint protegido rechaza con 403 los roles que no corresponden (o la falta de rol).
@AutoConfigureMockMvc
class SeguridadEndpointsTest extends VentasTestBase {

    @Autowired MockMvc mvc;

    @ParameterizedTest(name = "{0} {1} como {2} -> 403")
    @CsvSource({
            "POST,   /reservas,                  SIN_ROL",
            "POST,   /reservas,                  ADMIN",
            "POST,   /reservas,                  ORGANIZADOR",
            "POST,   /reservas,                  ACCESO",
            "GET,    /reservas/1,                ADMIN",
            "POST,   /reservas/1/pago,           ORGANIZADOR",
            "DELETE, /reservas/1,                ACCESO",
            "GET,    /entradas,                  ADMIN",
            "GET,    /entradas/1,                ORGANIZADOR",
            "POST,   /devoluciones,              ADMIN",
            "GET,    /devoluciones,              CLIENTE",
            "GET,    /devoluciones,              SIN_ROL",
            "PUT,    /devoluciones/1/aprobacion, CLIENTE",
            "PUT,    /devoluciones/1/rechazo,    ORGANIZADOR",
    })
    void rolesNoPermitidosReciben403(String metodo, String ruta, String rol) throws Exception {
        MockHttpServletRequestBuilder pedido = request(HttpMethod.valueOf(metodo), ruta)
                .header("X-Usuario-Id", "1")
                .header("Idempotency-Key", "clave-seguridad")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoValido(ruta));
        if (!"SIN_ROL".equals(rol)) {
            pedido.header("X-Rol", rol);
        }
        mvc.perform(pedido).andExpect(status().isForbidden());
    }

    @Test
    void elClienteVeSusEntradasYElAdministradorLasDevoluciones() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/entradas").header("X-Usuario-Id", "1").header("X-Rol", "CLIENTE"))
                .andExpect(status().isOk());
        mvc.perform(request(HttpMethod.GET, "/devoluciones").header("X-Rol", "ADMIN"))
                .andExpect(status().isOk());
    }

    // Cuerpos validos para que el rechazo sea por el rol y no por la validacion del cuerpo
    private static String cuerpoValido(String ruta) {
        if (ruta.equals("/reservas")) {
            return "{\"eventoId\":1,\"sectorId\":1,\"cantidad\":1}";
        }
        if (ruta.endsWith("/pago")) {
            return "{\"medioPago\":\"TARJETA\"}";
        }
        if (ruta.equals("/devoluciones")) {
            return "{\"entradaId\":1,\"motivo\":\"No puedo ir\"}";
        }
        return "{}";
    }
}
