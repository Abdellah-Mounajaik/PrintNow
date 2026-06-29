package com.printnow.module.shop.controller;

import com.printnow.module.shop.dto.HoraireOuvertureRequestDTO;
import com.printnow.module.shop.dto.HoraireOuvertureResponseDTO;
import com.printnow.module.shop.service.HoraireOuvertureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/horaires")
@RequiredArgsConstructor
public class HoraireOuvertureController {

    private final HoraireOuvertureService horaireService;

    @PutMapping("/{id}")
    public ResponseEntity<HoraireOuvertureResponseDTO> updateHoraire(
            @PathVariable Long id,
            @RequestBody HoraireOuvertureRequestDTO dto) {
        return ResponseEntity.ok(horaireService.updateHoraire(id, dto));
    }
}
