package com.ticketya.catalogo.mensajeria;

// Lo unico que catalogo necesita de los mensajes "compra.confirmada" y "devolucion.aprobada" que publica ventas:
// de que evento se trata (el resto de los campos del JSON se ignora)
public record CambioDeVentasEvento(Long eventoId) {
}
