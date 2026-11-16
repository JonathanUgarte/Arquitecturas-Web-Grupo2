package com.ticketya.notificaciones.canal;

// Medio por el que sale el aviso (email, push). Es una interfaz para poder cambiar el proveedor real
// (SendGrid, Firebase, etc.) sin tocar la logica.
public interface CanalDeEnvio {

    String nombre();

    void enviar(Long usuarioId, String titulo, String mensaje);
}
