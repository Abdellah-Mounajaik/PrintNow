package com.printnow.module.promo.controller;

import com.printnow.module.promo.dto.CodePromoRequestDTO;
import com.printnow.module.promo.dto.CodePromoResponseDTO;
import com.printnow.module.promo.service.CodePromoService;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    @GetMapping("/valider")
    public ResponseEntity<?> valider(
            @RequestParam String code,
            @RequestParam BigDecimal montant) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User client = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));
        try {
            return ResponseEntity.ok(codePromoService.validerCode(code, montant, client.getId()));
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
