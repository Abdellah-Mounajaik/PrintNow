package com.printnow.module.shop.service;

import com.printnow.infrastructure.security.UtilisateurCourant;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.repository.HoraireOuvertureRepository;
import com.printnow.module.shop.repository.ImprimerieRepository;
import com.printnow.module.shop.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Vérifie qu'on a le droit de modifier une boutique.
 *
 * Le catalogue se consulte librement, mais le modifier ne regarde que son gérant
 * et l'administration. Sans ce contrôle, tout imprimeur connecté pourrait
 * changer les tarifs, les horaires ou le nom d'un concurrent.
 */
@Service
@RequiredArgsConstructor
public class DroitsImprimerieService {

    private final ImprimerieRepository imprimerieRepository;
    private final ProduitRepository produitRepository;
    private final HoraireOuvertureRepository horaireRepository;
    private final UtilisateurCourant utilisateurCourant;

    /** @throws ResponseStatusException 403 si la boutique n'est pas la sienne */
    @Transactional(readOnly = true)
    public void verifierAccesImprimerie(Long imprimerieId) {
        if (estAdmin()) return;

        Imprimerie imprimerie = imprimerieRepository.findById(imprimerieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imprimerie introuvable."));
        refuserSiCeNestPasSonImprimerie(imprimerie);
    }

    @Transactional(readOnly = true)
    public void verifierAccesProduit(Long produitId) {
        if (estAdmin()) return;

        refuserSiCeNestPasSonImprimerie(produitRepository.findById(produitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable."))
                .getImprimerie());
    }

    @Transactional(readOnly = true)
    public void verifierAccesHoraire(Long horaireId) {
        if (estAdmin()) return;

        refuserSiCeNestPasSonImprimerie(horaireRepository.findById(horaireId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horaire introuvable."))
                .getImprimerie());
    }

    private void refuserSiCeNestPasSonImprimerie(Imprimerie imprimerie) {
        Long moi = utilisateurCourant.id();
        if (imprimerie == null || imprimerie.getGerant() == null
                || !imprimerie.getGerant().getId().equals(moi)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette boutique n'est pas la vôtre.");
        }
    }

    private boolean estAdmin() {
        return utilisateurCourant.estAdmin();
    }
}
