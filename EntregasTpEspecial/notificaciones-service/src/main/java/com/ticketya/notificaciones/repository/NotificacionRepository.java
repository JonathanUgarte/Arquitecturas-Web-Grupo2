package com.ticketya.notificaciones.repository;

import com.ticketya.notificaciones.model.EstadoNotificacion;
import com.ticketya.notificaciones.model.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    List<Notificacion> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    List<Notificacion> findByEstadoAndIntentosLessThan(EstadoNotificacion estado, int intentos);
}
