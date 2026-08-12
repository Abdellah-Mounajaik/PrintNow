package com.printnow.module.shop.controller;

import com.printnow.module.shop.dto.HoraireOuvertureRequestDTO;
import com.printnow.module.shop.dto.HoraireOuvertureResponseDTO;
import com.printnow.module.shop.service.DroitsImprimerieService;
import com.printnow.module.shop.service.HoraireOuvertureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/horaires")
@RequiredArgsConstructor
public class HoraireOuvertureController {

    private final HoraireOuvertureService horaireService;
    private final DroitsImprimerieService droits;

    /** Seul le gérant de la boutique change ses horaires. */
    @PutMapping("/{id}")
    public ResponseEntity<HoraireOuvertureResponseDTO> updateHoraire(
            @PathVariable Long id,
            @RequestBody HoraireOuvertureRequestDTO dto) {
        droits.verifierAccesHoraire(id);
        return ResponseEntity.ok(horaireService.updateHoraire(id, dto));
    }
}
