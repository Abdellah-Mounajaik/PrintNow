package com.printnow.module.parametres.controller;

import com.printnow.module.parametres.dto.ParametresPlateformeDTO;
import com.printnow.module.parametres.model.ParametresPlateforme;
import com.printnow.module.parametres.service.ParametresService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parametres")
@RequiredArgsConstructor
public class ParametresController {

    private final ParametresService parametresService;

    /** Public : ces tarifs sont affichés sur la page "Devenir partenaire" et dans le chatbot. */
    @GetMapping
    public ResponseEntity<ParametresPlateforme> getParametres() {
        return ResponseEntity.ok(parametresService.getParametres());
    }

    /** Réservé à l'administration. */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParametresPlateforme> mettreAJour(@RequestBody ParametresPlateformeDTO dto) {
        return ResponseEntity.ok(parametresService.mettreAJour(dto));
    }
}
