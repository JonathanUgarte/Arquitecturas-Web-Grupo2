package com.ticketya.preferencias.repository;

import com.ticketya.preferencias.model.PerfilCliente;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface PerfilRepository extends MongoRepository<PerfilCliente, Long> {

    // Clientes que compraron alguno de estos eventos (consulta sobre el arreglo embebido "compras")
    List<PerfilCliente> findByComprasEventoIdIn(Collection<Long> eventoIds);

    // Clientes de una ciudad con alguno de estos artistas entre sus favoritos ($in sobre un arreglo)
    List<PerfilCliente> findByCiudadNormalizadaAndArtistasNormalizadosIn(String ciudad, Collection<String> artistas);
}
