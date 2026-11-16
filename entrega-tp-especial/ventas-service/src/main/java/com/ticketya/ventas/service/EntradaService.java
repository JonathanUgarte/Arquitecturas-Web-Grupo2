package com.ticketya.ventas.service;

import com.ticketya.ventas.dto.EntradaDTO;
import com.ticketya.ventas.exception.ForbiddenException;
import com.ticketya.ventas.exception.NotFoundException;
import com.ticketya.ventas.model.Entrada;
import com.ticketya.ventas.repository.EntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntradaService {

    private final EntradaRepository entradaRepo;

    @Transactional(readOnly = true)
    public List<EntradaDTO> listar(Long usuarioId) {
        return entradaRepo.findByUsuarioIdOrderByFechaCompraDesc(usuarioId).stream()
                .map(Mapeos::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EntradaDTO obtener(Long usuarioId, Long entradaId) {
        Entrada e = entradaRepo.findById(entradaId)
                .orElseThrow(() -> new NotFoundException("No existe la entrada " + entradaId));
        if (!e.getUsuarioId().equals(usuarioId)) {
            throw new ForbiddenException("La entrada no pertenece al usuario");
        }
        return Mapeos.toDTO(e);
    }
}
