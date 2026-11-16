package com.ticketya.catalogo.service;

import com.ticketya.catalogo.dto.LugarDTO;
import com.ticketya.catalogo.dto.LugarRequest;
import com.ticketya.catalogo.dto.SectorDTO;
import com.ticketya.catalogo.dto.SectorRequest;
import com.ticketya.catalogo.exception.BadRequestException;
import com.ticketya.catalogo.exception.NotFoundException;
import com.ticketya.catalogo.model.Lugar;
import com.ticketya.catalogo.model.Sector;
import com.ticketya.catalogo.model.TipoSector;
import com.ticketya.catalogo.repository.LugarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LugarService {

    private final LugarRepository lugarRepo;

    @Transactional
    public LugarDTO crear(LugarRequest req) {
        Lugar lugar = new Lugar();
        lugar.setNombre(req.nombre());
        lugar.setDireccion(req.direccion());
        lugar.setCiudad(req.ciudad());
        lugar.setLatitud(req.latitud());
        lugar.setLongitud(req.longitud());

        for (SectorRequest sr : req.sectores()) {
            validarSector(sr);
            Sector sector = new Sector();
            sector.setLugar(lugar);
            sector.setNombre(sr.nombre());
            sector.setTipo(sr.tipo());
            sector.setCapacidad(sr.capacidad());
            sector.setFilas(sr.filas());
            sector.setAsientosPorFila(sr.asientosPorFila());
            lugar.getSectores().add(sector);
        }
        return toDTO(lugarRepo.save(lugar));
    }

    @Transactional(readOnly = true)
    public LugarDTO obtener(Long id) {
        Lugar lugar = lugarRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el lugar " + id));
        return toDTO(lugar);
    }

    @Transactional(readOnly = true)
    public List<LugarDTO> listar() {
        return lugarRepo.findAll().stream().map(LugarService::toDTO).toList();
    }

    // Un sector NUMERADO necesita filas y asientos por fila; uno NO_NUMERADO, una capacidad.
    private void validarSector(SectorRequest sr) {
        if (sr.tipo() == TipoSector.NUMERADO) {
            if (sr.filas() == null || sr.filas() <= 0 || sr.asientosPorFila() == null || sr.asientosPorFila() <= 0) {
                throw new BadRequestException("El sector numerado '" + sr.nombre() + "' necesita filas y asientosPorFila positivos");
            }
        } else {
            if (sr.capacidad() == null || sr.capacidad() <= 0) {
                throw new BadRequestException("El sector no numerado '" + sr.nombre() + "' necesita una capacidad positiva");
            }
        }
    }

    public static LugarDTO toDTO(Lugar l) {
        return new LugarDTO(l.getId(), l.getNombre(), l.getDireccion(), l.getCiudad(),
                l.getLatitud(), l.getLongitud(),
                l.getSectores().stream().map(LugarService::toDTO).toList());
    }

    public static SectorDTO toDTO(Sector s) {
        return new SectorDTO(s.getId(), s.getNombre(), s.getTipo(), s.getCapacidad(),
                s.getFilas(), s.getAsientosPorFila(), s.capacidadTotal());
    }
}
