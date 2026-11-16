package com.ticketya.catalogo;

import com.ticketya.catalogo.client.VentasClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Segunda barrera de autorizacion: aunque un pedido llegara al servicio sin pasar por el gateway,
// cada endpoint protegido rechaza con 403 los roles que no corresponden (o la falta de rol).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadEndpointsTest {

    @MockitoBean
    VentasClient ventasClient;

    @Autowired MockMvc mvc;

    @ParameterizedTest(name = "{0} {1} como {2} -> 403")
    @CsvSource({
            "POST, /lugares,                 SIN_ROL",
            "POST, /lugares,                 CLIENTE",
            "POST, /lugares,                 ACCESO",
            "POST, /eventos,                 SIN_ROL",
            "POST, /eventos,                 CLIENTE",
            "PUT,  /eventos/1/precios,       CLIENTE",
            "POST, /eventos/1/publicacion,   CLIENTE",
            "POST, /eventos/1/publicacion,   ACCESO",
            "PUT,  /eventos/1/suspension,    SIN_ROL",
            "PUT,  /eventos/1/suspension,    ORGANIZADOR",
            "PUT,  /eventos/1/cancelacion,   CLIENTE",
            "PUT,  /eventos/1/cancelacion,   ORGANIZADOR",
            "GET,  /eventos/1/reporte,       CLIENTE",
            "GET,  /eventos/1/reporte,       SIN_ROL",
            "GET,  /eventos/mis-eventos,     CLIENTE",
            "GET,  /eventos/mis-eventos,     ACCESO",
    })
    void rolesNoPermitidosReciben403(String metodo, String ruta, String rol) throws Exception {
        MockHttpServletRequestBuilder pedido = request(HttpMethod.valueOf(metodo), ruta)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoValido(ruta));
        if (!"SIN_ROL".equals(rol)) {
            pedido.header("X-Rol", rol);
        }
        mvc.perform(pedido).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "GET {0} sin rol -> 200")
    @CsvSource({"/eventos", "/lugares"})
    void lasConsultasSonPublicas(String ruta) throws Exception {
        mvc.perform(request(HttpMethod.GET, ruta)).andExpect(status().isOk());
    }

    // Cuerpos validos para que el rechazo sea por el rol y no por la validacion del cuerpo
    private static String cuerpoValido(String ruta) {
        if (ruta.equals("/lugares")) {
            return "{\"nombre\":\"Sala\",\"direccion\":\"Calle 1\",\"ciudad\":\"Tandil\",\"latitud\":-37.3,\"longitud\":-59.1,"
                    + "\"sectores\":[{\"nombre\":\"Campo\",\"tipo\":\"NO_NUMERADO\",\"capacidad\":100}]}";
        }
        if (ruta.equals("/eventos")) {
            return "{\"nombre\":\"Show\",\"descripcion\":\"x\",\"tipo\":\"RECITAL\",\"organizador\":\"Prod\","
                    + "\"participantes\":[\"Banda\"],\"fechaHora\":\"2030-01-01T21:00:00\",\"lugarId\":1}";
        }
        if (ruta.endsWith("/precios")) {
            return "{\"precios\":[{\"sectorId\":1,\"precio\":1000}]}";
        }
        if (ruta.endsWith("/cancelacion")) {
            return "{\"motivo\":\"Lluvia\"}";
        }
        return "{}";
    }
}
