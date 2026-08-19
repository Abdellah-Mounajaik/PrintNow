package com.printnow.module.shop.controller;

import com.printnow.infrastructure.security.UtilisateurCourant;
import com.printnow.module.order.service.FactureInscriptionService;
import com.printnow.module.shop.dto.ImprimerieRequestDTO;
import com.printnow.module.shop.dto.ImprimerieResponseDTO;
import com.printnow.module.shop.service.DroitsImprimerieService;
import com.printnow.module.shop.service.ImprimerieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Le catalogue se consulte librement ; le modifier ne regarde que le gérant de
 * la boutique et l'administration. Les inscriptions de partenaires passent par
 * /api/partners/register, qui reste public.
 */
@RestController
@RequestMapping("/api/imprimeries")
@RequiredArgsConstructor
public class ImprimerieController {

    private final ImprimerieService imprimerieService;
    private final DroitsImprimerieService droits;
    private final FactureInscriptionService factureInscriptionService;
    private final UtilisateurCourant utilisateurCourant;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImprimerieResponseDTO> createImprimerie(@RequestBody ImprimerieRequestDTO dto) {
        return new ResponseEntity<>(imprimerieService.createImprimerie(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ImprimerieResponseDTO>> getAllActiveImprimeries() {
        return ResponseEntity.ok(imprimerieService.getAllActiveImprimeries());
    }

    /**
     * GET /api/imprimeries/admin/toutes
     * Toutes les imprimeries, y compris fermées : réservé à l'administration
     * (le catalogue public ci-dessus ne doit montrer que les actives).
     */
    @GetMapping("/admin/toutes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ImprimerieResponseDTO>> getToutesImprimeries() {
        return ResponseEntity.ok(imprimerieService.getToutesImprimeries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImprimerieResponseDTO> getImprimerieById(@PathVariable Long id) {
        return ResponseEntity.ok(imprimerieService.getImprimerieById(id));
    }

    /**
     * Récupère une imprimerie depuis l'adresse lisible de sa fiche
     * (« imprimerie-du-centre »), telle qu'elle apparaît dans le navigateur.
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ImprimerieResponseDTO> getImprimerieBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(imprimerieService.getImprimerieBySlug(slug));
    }

    /** Récupère l'imprimerie du gérant connecté (dashboard imprimeur). */
    @GetMapping("/gerant/{idGerant}")
    public ResponseEntity<ImprimerieResponseDTO> getImprimerieByGerant(@PathVariable Long idGerant) {
        return ResponseEntity.ok(imprimerieService.getImprimerieByGerantId(idGerant));
    }

    /** Modification réservée au gérant de cette boutique, ou à l'administration. */
    @PutMapping("/{id}")
    public ResponseEntity<ImprimerieResponseDTO> updateImprimerie(@PathVariable Long id, @RequestBody ImprimerieRequestDTO dto) {
        droits.verifierAccesImprimerie(id);
        return ResponseEntity.ok(imprimerieService.updateImprimerie(id, dto));
    }

    /**
     * DELETE /api/imprimeries/{id}
     * Ferme une boutique : elle quitte le catalogue et ne reçoit plus de
     * commandes. Ses données restent en base — ses commandes passées y renvoient.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteImprimerie(@PathVariable Long id) {
        imprimerieService.deleteImprimerie(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/imprimeries/{id}/facture-inscription
     * Télécharge la facture PDF des frais d'inscription de cette imprimerie.
     * Accessible à l'admin (n'importe quelle imprimerie) ou au gérant propriétaire.
     */
    @GetMapping("/{id}/facture-inscription")
    @PreAuthorize("hasRole('ADMIN') or hasRole('IMPRIMERIE')")
    public ResponseEntity<byte[]> telechargerFactureInscription(@PathVariable Long id) {
        Long gerantId = utilisateurCourant.estAdmin() ? null : utilisateurCourant.id();
        byte[] pdf = factureInscriptionService.genererFacture(id, gerantId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"facture-inscription-" + id + ".pdf\"")
                .body(pdf);
    }
}