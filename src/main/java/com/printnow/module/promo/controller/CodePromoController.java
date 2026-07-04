package com.printnow.module.promo.controller;

import com.printnow.module.promo.dto.CodePromoRequestDTO;
import com.printnow.module.promo.dto.CodePromoResponseDTO;
import com.printnow.module.promo.service.CodePromoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promos")
@RequiredArgsConstructor
public class CodePromoController {

    private final CodePromoService codePromoService;

    @GetMapping("/valider")
    public ResponseEntity<?> valider(
            @RequestParam String code,
            @RequestParam BigDecimal montant) {
        try {
            return ResponseEntity.ok(codePromoService.validerCode(code, montant));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("message", e.getReason() != null ? e.getReason() : "Code invalide"));
        }
    }

    @GetMapping("/imprimerie/{imprimerieId}")
    public ResponseEntity<List<CodePromoResponseDTO>> getCodesImprimerie(@PathVariable Long imprimerieId) {
        return ResponseEntity.ok(codePromoService.getCodesForImprimerie(imprimerieId));
    }

    @PostMapping
    public ResponseEntity<CodePromoResponseDTO> creerCode(@RequestBody CodePromoRequestDTO dto) {
        return ResponseEntity.ok(codePromoService.creerCode(dto));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CodePromoResponseDTO> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(codePromoService.toggleActif(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerCode(@PathVariable Long id) {
        codePromoService.supprimerCode(id);
        return ResponseEntity.noContent().build();
    }
}
