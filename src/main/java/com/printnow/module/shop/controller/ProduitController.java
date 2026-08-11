package com.printnow.module.shop.controller;

import com.printnow.module.shop.dto.ProduitRequestDTO;
import com.printnow.module.shop.dto.ProduitResponseDTO;
import com.printnow.module.shop.service.DroitsImprimerieService;
import com.printnow.module.shop.service.ProduitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Le catalogue se consulte librement ; seul le gérant de la boutique — ou
 * l'administration — peut y toucher. Sans ce contrôle, n'importe qui pouvait
 * modifier les tarifs d'un concurrent.
 */
@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
public class ProduitController {

    private final ProduitService produitService;
    private final DroitsImprimerieService droits;

    @PostMapping
    public ResponseEntity<ProduitResponseDTO> createProduit(@RequestBody ProduitRequestDTO dto) {
        droits.verifierAccesImprimerie(dto.getImprimerieId());
        return new ResponseEntity<>(produitService.createProduit(dto), HttpStatus.CREATED);
    }

    // Récupérer le catalogue d'une imprimerie spécifique
    @GetMapping("/imprimerie/{imprimerieId}")
    public ResponseEntity<List<ProduitResponseDTO>> getCatalogueByImprimerie(@PathVariable Long imprimerieId) {
        return ResponseEntity.ok(produitService.getCatalogueByImprimerie(imprimerieId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProduitResponseDTO> updateProduit(@PathVariable Long id, @RequestBody ProduitRequestDTO dto) {
        droits.verifierAccesProduit(id);
        return ResponseEntity.ok(produitService.updateProduit(id, dto));
    }

    // Désactiver un produit plutôt que de le supprimer physiquement
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactiverProduit(@PathVariable Long id) {
        droits.verifierAccesProduit(id);
        produitService.desactiverProduit(id);
        return ResponseEntity.noContent().build();
    }
}