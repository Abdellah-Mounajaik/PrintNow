package com.printnow.module.user.service;

import com.printnow.infrastructure.email.EmailService;
import com.printnow.module.user.dto.ReinitialisationRequestDTO;
import com.printnow.module.user.model.JetonReinitialisation;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Réinitialisation du mot de passe par email.
 *
 * Le principe : l'utilisateur qui a perdu son mot de passe prouve qu'il possède
 * bien la boîte mail du compte, en cliquant sur un lien qu'on n'envoie qu'à
 * cette adresse. Le lien porte un jeton aléatoire à usage unique, valable une
 * demi-heure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReinitialisationMotDePasseService {

    /** Durée de validité du lien, reprise telle quelle dans l'email. */
    public static final Duration VALIDITE = Duration.ofMinutes(30);

    /**
     * Délai avant de renvoyer un lien à la même adresse. Sans lui, ce formulaire
     * public permettrait de noyer la boîte mail de n'importe quel inscrit.
     */
    private static final Duration DELAI_ENTRE_DEUX_ENVOIS = Duration.ofMinutes(2);

    /** 32 octets : deviner un jeton par tâtonnement est hors de portée. */
    private static final int OCTETS_DU_JETON = 32;

    private final UserRepository userRepository;
    private final JetonReinitialisationRepository jetonRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom alea = new SecureRandom();

    /**
     * Envoie un lien de réinitialisation, si l'adresse correspond à un compte.
     *
     * Ne dit jamais si l'adresse est connue : la réponse est identique dans tous
     * les cas. Autrement, ce formulaire public dirait à n'importe qui quelles
     * adresses sont inscrites sur PrintNow.
     */
    @Transactional
    public void demanderUnLien(String email) {
        LocalDateTime maintenant = LocalDateTime.now();
        purgerLesJetonsPerimes(maintenant);

        Optional<User> compte = userRepository.findByEmail(email.trim());
        if (compte.isEmpty()) {
            log.info("Demande de réinitialisation pour une adresse inconnue — aucun email envoyé");
            return;
        }

        User utilisateur = compte.get();
        if (Boolean.FALSE.equals(utilisateur.getActif())) {
            log.info("Demande de réinitialisation pour le compte désactivé {} — aucun email envoyé", utilisateur.getId());
            return;
        }

        List<JetonReinitialisation> enCours = jetonRepository.findByUtilisateurAndUtiliseLeIsNull(utilisateur);
        if (envoiTropRecent(enCours, maintenant)) {
            log.info("Lien déjà envoyé il y a moins de {} minutes au compte {} — envoi ignoré",
                    DELAI_ENTRE_DEUX_ENVOIS.toMinutes(), utilisateur.getId());
            return;
        }

        // Un seul lien valable à la fois : demander un nouveau lien annule le
        // précédent, y compris s'il a été intercepté.
        jetonRepository.deleteAll(enCours);

        String jeton = engendrerUnJeton();
        JetonReinitialisation enregistrement = new JetonReinitialisation();
        enregistrement.setEmpreinte(empreinte(jeton));
        enregistrement.setUtilisateur(utilisateur);
        enregistrement.setCreeLe(maintenant);
        enregistrement.setExpireLe(maintenant.plus(VALIDITE));
        jetonRepository.save(enregistrement);

        emailService.envoyerReinitialisationMotDePasse(
                utilisateur.getEmail(), utilisateur.getPrenom(), jeton, VALIDITE.toMinutes());
    }

    /**
     * Remplace le mot de passe du compte désigné par le jeton.
     *
     * @throws ResponseStatusException 400 si le lien est inconnu, périmé ou déjà utilisé
     */
    @Transactional
    public void reinitialiser(ReinitialisationRequestDTO demande) {
        LocalDateTime maintenant = LocalDateTime.now();

        JetonReinitialisation jeton = jetonRepository.findByEmpreinte(empreinte(demande.getJeton()))
                .filter(candidat -> candidat.estUtilisable(maintenant))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ce lien de réinitialisation n'est plus valable. Demandez-en un nouveau."));

        User utilisateur = jeton.getUtilisateur();
        utilisateur.setMotDePasse(passwordEncoder.encode(demande.getNouveauMotDePasse()));
        userRepository.save(utilisateur);

        // Le jeton est marqué avant tout le reste : un même lien ne peut pas
        // changer le mot de passe deux fois.
        jeton.setUtiliseLe(maintenant);
        jetonRepository.save(jeton);

        log.info("Mot de passe réinitialisé pour le compte {}", utilisateur.getId());
    }

    /** Indique si le lien reçu est toujours exploitable, sans rien modifier. */
    public boolean lienEncoreValable(String jeton) {
        if (jeton == null || jeton.isBlank()) return false;
        return jetonRepository.findByEmpreinte(empreinte(jeton))
                .filter(candidat -> candidat.estUtilisable(LocalDateTime.now()))
                .isPresent();
    }

    private boolean envoiTropRecent(List<JetonReinitialisation> enCours, LocalDateTime maintenant) {
        return enCours.stream().anyMatch(jeton ->
                jeton.getCreeLe().isAfter(maintenant.minus(DELAI_ENTRE_DEUX_ENVOIS)));
    }

    private void purgerLesJetonsPerimes(LocalDateTime maintenant) {
        try {
            jetonRepository.deleteByExpireLeBefore(maintenant);
        } catch (RuntimeException e) {
            // La purge n'est qu'un entretien : son échec ne doit pas priver
            // l'utilisateur de son lien.
            log.warn("Purge des jetons de réinitialisation périmés impossible", e);
        }
    }

    private String engendrerUnJeton() {
        byte[] octets = new byte[OCTETS_DU_JETON];
        alea.nextBytes(octets);
        // Sans padding ni caractère à échapper : le jeton voyage dans une URL.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    private String empreinte(String jeton) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(jeton.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible sur cette machine", e);
        }
    }
}
