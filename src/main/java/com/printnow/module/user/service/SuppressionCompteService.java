package com.printnow.module.user.service;

import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.model.AdresseLivraison;
import com.printnow.module.order.model.Commande;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.order.service.ArchiveFactureService;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.repository.ImprimerieRepository;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.JetonReinitialisationRepository;
import com.printnow.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Suppression d'un compte utilisateur.
 *
 * La ligne n'est pas détruite : commandes, factures et avis y renvoient, et la
 * loi belge impose de conserver les factures sept ans. Ce sont les données
 * personnelles qui sont effacées — c'est ce que demande le droit à l'effacement,
 * et cela suffit : une facture rattachée à « Compte supprimé » ne désigne plus
 * personne.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuppressionCompteService {

    /**
     * Commandes pour lesquelles l'imprimeur a encore du travail : supprimer le
     * compte couperait le seul lien permettant de joindre son client.
     *
     * PRETE en est volontairement absent. Une commande prête est une commande
     * honorée : elle attend seulement que le client passe la prendre, ce que le
     * parcours en retrait magasin n'enregistre pas. L'y inclure bloquerait donc
     * la suppression indéfiniment, sans que personne puisse y remédier.
     */
    private static final List<StatutCommande> COMMANDES_EN_COURS = List.of(
            StatutCommande.PAYEE,
            StatutCommande.EN_COURS_IMPRESSION);

    /** Domaine réservé par la RFC 2606 : aucun message ne pourra jamais y partir. */
    private static final String DOMAINE_NEUTRE = "@printnow.invalid";

    /** Les colonnes d'adresse sont non nulles : on les vide sans les annuler. */
    private static final String CHAMP_EFFACE = "-";
    private static final String DESTINATAIRE_EFFACE = "Destinataire supprimé";

    private final UserRepository userRepository;
    private final CommandeRepository commandeRepository;
    private final ImprimerieRepository imprimerieRepository;
    private final JetonReinitialisationRepository jetonRepository;
    private final ArchiveFactureService archiveFactureService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Supprime le compte : ses données personnelles sont remplacées par des
     * valeurs neutres et il ne peut plus se connecter.
     *
     * @throws ResponseStatusException 409 s'il reste des commandes en cours
     */
    @Transactional
    public void supprimer(Long userId, String demandePar) {
        User utilisateur = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        if (utilisateur.estSupprime()) return; // déjà fait : rien à refaire

        refuserSiDernierAdmin(utilisateur);
        refuserSiCommandesEnCours(utilisateur);
        fermerLesImprimeriesGerees(utilisateur);

        // Les factures sont figées avant l'effacement : elles doivent porter le
        // nom du client pendant sept ans, alors qu'elles sont d'ordinaire
        // reconstruites depuis des données qui vont disparaître.
        archiveFactureService.archiverLesFacturesDe(utilisateur);
        anonymiserLesAdressesDeLivraison(utilisateur);

        // Les liens de réinitialisation encore valables deviendraient sinon un
        // moyen de reprendre la main sur un compte supprimé.
        jetonRepository.deleteAll(jetonRepository.findByUtilisateurAndUtiliseLeIsNull(utilisateur));

        anonymiser(utilisateur);
        userRepository.save(utilisateur);

        log.info("Compte {} supprimé et anonymisé (demande de {})", userId, demandePar);
    }

    /**
     * Empêche la disparition du dernier administrateur : plus personne ne
     * pourrait alors accéder au tableau de bord, ni rendre la main à qui que ce
     * soit.
     */
    private void refuserSiDernierAdmin(User utilisateur) {
        if (utilisateur.getRole() == null || !"ADMIN".equalsIgnoreCase(utilisateur.getRole().getNom())) return;

        long autresAdmins = userRepository.findByRoleNom("ADMIN").stream()
                .filter(admin -> !admin.getId().equals(utilisateur.getId()))
                .filter(admin -> !admin.estSupprime())
                .count();

        if (autresAdmins == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "C'est le dernier compte administrateur : le supprimer fermerait l'accès au tableau de bord. "
                            + "Créez un autre administrateur d'abord.");
        }
    }

    private void refuserSiCommandesEnCours(User utilisateur) {
        long enCours = commandeRepository.countByClient_IdAndStatutIn(utilisateur.getId(), COMMANDES_EN_COURS);
        if (enCours > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, enCours > 1
                    ? String.format("Ce compte a %d commandes en cours d'impression. "
                            + "La suppression sera possible une fois qu'elles seront prêtes ou annulées.", enCours)
                    : "Ce compte a une commande en cours d'impression. "
                            + "La suppression sera possible une fois qu'elle sera prête ou annulée.");
        }
    }

    /**
     * Ferme les imprimeries dont ce compte est le gérant : sans gérant, elles
     * continueraient à figurer au catalogue et à encaisser des commandes que
     * plus personne ne traite.
     */
    private void fermerLesImprimeriesGerees(User utilisateur) {
        List<Imprimerie> gerees = imprimerieRepository.findByGerantId(utilisateur.getId());
        for (Imprimerie imprimerie : gerees) {
            if (Boolean.TRUE.equals(imprimerie.getActif())) {
                imprimerie.setActif(false);
                imprimerieRepository.save(imprimerie);
                log.info("Imprimerie {} fermée : son gérant {} a été supprimé",
                        imprimerie.getId(), utilisateur.getId());
            }
        }
    }

    /**
     * Efface ce qui désigne la personne dans ses adresses de livraison.
     *
     * Anonymiser le compte sans y toucher ne servirait à rien : le nom, le
     * téléphone et l'adresse exacte y subsistent et suffisent à identifier
     * quelqu'un. La commune et le code postal sont conservés — ils ne désignent
     * personne et gardent leur utilité statistique.
     */
    private void anonymiserLesAdressesDeLivraison(User utilisateur) {
        List<Commande> commandes = commandeRepository.findByClient_IdOrderByDateCreationDesc(utilisateur.getId());
        int anonymisees = 0;

        for (Commande commande : commandes) {
            AdresseLivraison adresse = commande.getAdresseLivraison();
            if (adresse == null) continue; // retrait en magasin

            adresse.setNomDestinataire(DESTINATAIRE_EFFACE);
            adresse.setTelephone(CHAMP_EFFACE);
            adresse.setRue(CHAMP_EFFACE);
            adresse.setNumero(CHAMP_EFFACE);
            // Les coordonnées pointeraient sinon le domicile au mètre près.
            adresse.setLatitude(null);
            adresse.setLongitude(null);
            commandeRepository.save(commande);
            anonymisees++;
        }

        if (anonymisees > 0) {
            log.info("{} adresse(s) de livraison anonymisée(s) pour le compte {}", anonymisees, utilisateur.getId());
        }
    }

    private void anonymiser(User utilisateur) {
        utilisateur.setPrenom("Compte");
        utilisateur.setNom("supprimé");
        // L'ancienne adresse est libérée : la personne pourra se réinscrire.
        utilisateur.setEmail("supprime-" + utilisateur.getId() + DOMAINE_NEUTRE);
        utilisateur.setTelephone(null);
        // Mot de passe rendu inutilisable : plus aucun mot de passe connu n'ouvre
        // ce compte, même si son indicateur d'activité était rétabli par erreur.
        utilisateur.setMotDePasse(passwordEncoder.encode(UUID.randomUUID().toString()));
        utilisateur.setActif(false);
        utilisateur.setDateSuppression(LocalDateTime.now());
    }
}
