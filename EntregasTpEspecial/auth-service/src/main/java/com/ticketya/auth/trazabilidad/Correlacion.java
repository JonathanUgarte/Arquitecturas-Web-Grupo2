package com.ticketya.auth.trazabilidad;

import org.slf4j.MDC;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.UUID;

// Trazabilidad entre servicios: el gateway asigna un X-Correlation-Id a cada pedido y todos los servicios lo
// propagan (en las llamadas HTTP y en los mensajes de RabbitMQ) y lo escriben en cada linea de log.
// Asi, en el log centralizado (Grafana + Loki) se ve el recorrido completo de un pedido por todos los servicios.
public final class Correlacion {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_CLAVE = "correlationId";

    private Correlacion() {
    }

    public static String actual() {
        return MDC.get(MDC_CLAVE);
    }

    public static String nuevoId() {
        return UUID.randomUUID().toString();
    }

    // Para las llamadas HTTP salientes (RestClient): reenvia el id de correlacion actual
    public static ClientHttpRequestInterceptor propagar() {
        return (request, body, execution) -> {
            String id = MDC.get(MDC_CLAVE);
            if (id != null && !request.getHeaders().containsKey(HEADER)) {
                request.getHeaders().add(HEADER, id);
            }
            return execution.execute(request, body);
        };
    }

    // Ejecuta una tarea con un id de correlacion en el log (por ejemplo, al consumir un mensaje)
    public static void conId(String id, Runnable tarea) {
        MDC.put(MDC_CLAVE, id == null || id.isBlank() ? nuevoId() : id);
        try {
            tarea.run();
        } finally {
            MDC.remove(MDC_CLAVE);
        }
    }
}
