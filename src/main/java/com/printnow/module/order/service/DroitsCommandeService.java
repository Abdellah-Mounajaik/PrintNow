package com.printnow.module.order.service;

import com.printnow.infrastructure.security.UtilisateurCourant;
import com.printnow.module.order.model.Commande;
import com.printnow.module.order.model.FichierPDF;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.order.repository.FichierPDFRepository;
import com.printnow.module.shop.model.Imprimerie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Vérifie qu'on a le droit de voir ou de faire avancer une commande.
 *
 * Une commande ne regarde que trois personnes : le client qui l'a passée,
 * l'imprimeur qui l'exécute, et l'administration. Sans ce contrôle, il suffisait
 * d'être connecté — un compte s'ouvre en trois champs — pour lire les commandes
 * de tout le site, télécharger les documents des autres clients, ou marquer
 * « prête » une commande qu'on n'imprime pas, ce qui envoie un courriel au
 * client au passage.
 *
 * Le pendant de {@link com.printnow.module.shop.service.DroitsImprimerieService}
 * pour le module commande.
 */
@Service
@RequiredArgsConstructor
public class DroitsCommandeService {

    private final CommandeRepository commandeRepository;
    private final FichierPDFRepository fichierPDFRepository;
    private final UtilisateurCourant utilisateurCourant;

    /**
     * Consultation : le client concerné, l'imprimeur qui l'honore, ou l'admin.
     *
     * @throws ResponseStatusException 403 si la commande ne le concerne pas
     */
    @Transactional(readOnly = true)
    public void verifierAccesCommande(Long commandeId) {
        if (utilisateurCourant.estAdmin()) return;

        Commande commande = charger(commandeId);
        Long moi = utilisateurCourant.id();
        if (!estLeClient(commande, moi) && !estLeGerant(commande.getImprimerie(), moi)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande ne vous concerne pas.");
        }
    }

    /**
     * Modification : l'imprimeur qui l'exécute, ou l'admin.
     *
     * Le client est volontairement exclu : avancer un statut ou déposer un
     * numéro de suivi relève de celui qui imprime, pas de celui qui commande.
     */
    @Transactional(readOnly = true)
    public void verifierGestionCommande(Long commandeId) {
        if (utilisateurCourant.estAdmin()) return;

        Commande commande = charger(commandeId);
        if (!estLeGerant(commande.getImprimerie(), utilisateurCourant.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande n'est pas la vôtre à gérer.");
        }
    }

    /** Un document se rattache à sa commande par sa ligne : mêmes ayants droit. */
    @Transactional(readOnly = true)
    public void verifierAccesFichier(Long fichierId) {
        if (utilisateurCourant.estAdmin()) return;

        FichierPDF fichier = fichierPDFRepository.findById(fichierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable."));
        if (fichier.getLigneCommande() == null || fichier.getLigneCommande().getCommande() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce document ne vous concerne pas.");
        }
        verifierAccesCommande(fichier.getLigneCommande().getCommande().getId());
    }

    private Commande charger(Long commandeId) {
        return commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable."));
    }

    private boolean estLeClient(Commande commande, Long moi) {
        return commande.getClient() != null && commande.getClient().getId().equals(moi);
    }

    private boolean estLeGerant(Imprimerie imprimerie, Long moi) {
        return imprimerie != null && imprimerie.getGerant() != null
                && imprimerie.getGerant().getId().equals(moi);
    }
}
