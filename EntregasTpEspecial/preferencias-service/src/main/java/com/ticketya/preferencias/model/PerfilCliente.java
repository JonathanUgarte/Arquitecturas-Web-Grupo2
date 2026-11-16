package com.ticketya.preferencias.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Un documento por cliente con sus preferencias y su historial de compras.
// Es un buen caso para una base de documentos: se lee y se escribe siempre entero, tiene listas de
// largo variable y su estructura puede crecer (nuevas preferencias) sin migrar un esquema.
@Document("perfiles")
@Getter
@Setter
@NoArgsConstructor
public class PerfilCliente {

    // El id del usuario (el mismo que en auth-service y ventas)
    @Id
    private Long usuarioId;

    // Control de concurrencia optimista: si dos escrituras se pisan, la segunda falla y se reintenta
    @Version
    private Long version;

    private String ciudad;
    private Set<String> tiposFavoritos = new LinkedHashSet<>();
    private Set<String> artistasFavoritos = new LinkedHashSet<>();

    // Copias en minusculas para buscar sin importar mayusculas ("Banda Uno" = "banda uno")
    @Indexed
    private String ciudadNormalizada;
    @Indexed
    private Set<String> artistasNormalizados = new LinkedHashSet<>();

    private List<CompraRegistrada> compras = new ArrayList<>();

    private LocalDateTime actualizado;

    public PerfilCliente(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}
