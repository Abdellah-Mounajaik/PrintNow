package com.printnow.module.promo.service;

import com.printnow.infrastructure.security.UtilisateurCourant;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.promo.dto.CodePromoRequestDTO;
import com.printnow.module.promo.dto.CodePromoResponseDTO;
import com.printnow.module.promo.enums.TypeReduction;
import com.printnow.module.promo.model.CodePromo;
import com.printnow.module.promo.repository.CodePromoRepository;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.repository.ImprimerieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodePromoService {

    private final CodePromoRepository codePromoRepository;
    private final ImprimerieRepository imprimerieRepository;
    private final CommandeRepository commandeRepository;
    private final UtilisateurCourant utilisateurCourant;

    public CodePromoResponseDTO validerCode(String code, BigDecimal montantCommande, Long clientId,
                                            Long imprimerieId) {
        CodePromo promo = findAndValidate(code, montantCommande, clientId, imprimerieId);
        return toDto(promo);
    }

    @Transactional
    public CodePromo appliquerCode(String code, BigDecimal montantCommande, Long clientId,
                                   Long imprimerieId) {
        CodePromo promo = findAndValidate(code, montantCommande, clientId, imprimerieId);
        promo.setUtilisationCourante(promo.getUtilisationCourante() + 1);
        return codePromoRepository.save(promo);
    }

    @Transactional(readOnly = true)
    public List<CodePromoResponseDTO> getCodesForImprimerie(Long imprimerieId) {
        refuserSiCeNestPasSaBoutique(imprimerieId);
        return codePromoRepository.findByImprimerie_IdAndSupprimeFalseOrderByIdDesc(imprimerieId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CodePromoResponseDTO creerCode(CodePromoRequestDTO dto) {
        refuserSiCeNestPasSaBoutique(dto.getImprimerieId());
        Imprimerie imprimerie = imprimerieRepository.findById(dto.getImprimerieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imprimerie introuvable"));

        codePromoRepository.findByCodeIgnoreCaseAndSupprimeTrue(dto.getCode().trim().toUpperCase())
                .ifPresent(ancien -> {
                    ancien.setCode(ancien.getCode() + "_DEL_" + System.currentTimeMillis());
                    codePromoRepository.save(ancien);
                });

        CodePromo promo = new CodePromo();
        promo.setCode(dto.getCode().trim().toUpperCase());
        promo.setTypeReduction(TypeReduction.valueOf(dto.getTypeReduction()));
        promo.setValeurReduction(dto.getValeurReduction());
        promo.setDateDebut(dto.getDateDebut());
        promo.setDateFin(dto.getDateFin());
        promo.setUtilisationMax(dto.getUtilisationMax());
        promo.setUtilisationCourante(0);
        promo.setMontantMinimumCommande(dto.getMontantMinimumCommande());
        promo.setActif(true);
        promo.setImprimerie(imprimerie);

        return toDto(codePromoRepository.save(promo));
    }

    @Transactional
    public CodePromoResponseDTO toggleActif(Long id) {
        CodePromo promo = charger(id);
        refuserSiCeNestPasSaBoutique(promo);
        promo.setActif(!promo.getActif());
        return toDto(codePromoRepository.save(promo));
    }

    @Transactional
    public void supprimerCode(Long id) {
        CodePromo promo = charger(id);
        refuserSiCeNestPasSaBoutique(promo);
        promo.setSupprime(true);
        promo.setActif(false);
        promo.setCode(promo.getCode() + "_DEL_" + System.currentTimeMillis());
        codePromoRepository.save(promo);
    }

    private CodePromo charger(Long id) {
        return codePromoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code promo introuvable"));
    }

    /**
     * Réserve la gestion des codes au gérant de la boutique.
     *
     * Aucune dérogation pour l'administration, contrairement aux autres contrôles
     * du projet : une réduction est consentie par le commerçant et sortie de sa
     * poche. Le contrôle vit ici, dans le service, et non dans le contrôleur :
     * ainsi il s'applique quel que soit le chemin par lequel on arrive.
     *
     * Sans lui, n'importe quel compte connecté pouvait s'émettre un code chez
     * l'imprimerie de son choix — et un compte s'ouvre en trois champs.
     */
    private void refuserSiCeNestPasSaBoutique(Long imprimerieId) {
        if (imprimerieId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'imprimerie est obligatoire.");
        }
        refuserSiCeNestPasSaBoutique(imprimerieRepository.findById(imprimerieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imprimerie introuvable")));
    }

    /** Un code appartient à la boutique qui l'a émis : c'est elle qui en décide. */
    private void refuserSiCeNestPasSaBoutique(CodePromo promo) {
        refuserSiCeNestPasSaBoutique(promo.getImprimerie());
    }

    private void refuserSiCeNestPasSaBoutique(Imprimerie imprimerie) {
        Long moi = utilisateurCourant.id();
        if (imprimerie == null || imprimerie.getGerant() == null
                || !imprimerie.getGerant().getId().equals(moi)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Les codes promo d'une boutique ne se gèrent que depuis celle-ci.");
        }
    }

    private CodePromo findAndValidate(String code, BigDecimal montantCommande, Long clientId,
                                      Long imprimerieId) {
        CodePromo promo = codePromoRepository.findByCodeIgnoreCaseAndActifTrueAndSupprimeFalse(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code promo invalide ou inexistant"));

        // Un code appartient à l'imprimerie qui l'a créé et ne vaut que chez
        // elle : c'est elle qui en supporte la remise. Sans ce contrôle, le code
        // d'une imprimerie réduisait la facture de n'importe quelle autre, aux
        // frais d'un commerçant qui n'avait jamais rien promis.
        if (promo.getImprimerie() != null && imprimerieId != null
                && !promo.getImprimerie().getId().equals(imprimerieId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce code promo n'est pas valable chez cette imprimerie.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getDateDebut() != null && now.isBefore(promo.getDateDebut()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce code n'est pas encore actif");
        if (promo.getDateFin() != null && now.isAfter(promo.getDateFin()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce code a expiré");
        if (promo.getUtilisationMax() != null) {
            long usagesParCetUtilisateur = commandeRepository.countByClient_IdAndCodePromo_Code(clientId, promo.getCode());
            if (usagesParCetUtilisateur >= promo.getUtilisationMax())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous avez déjà utilisé ce code le nombre maximum de fois autorisé");
        }
        if (promo.getMontantMinimumCommande() != null && montantCommande.compareTo(promo.getMontantMinimumCommande()) < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Montant minimum de " + promo.getMontantMinimumCommande() + "€ requis pour ce code");

        return promo;
    }

    private CodePromoResponseDTO toDto(CodePromo p) {
        CodePromoResponseDTO dto = new CodePromoResponseDTO();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setTypeReduction(p.getTypeReduction().name());
        dto.setValeurReduction(p.getValeurReduction());
        dto.setMontantMinimumCommande(p.getMontantMinimumCommande());
        dto.setDateDebut(p.getDateDebut());
        dto.setDateFin(p.getDateFin());
        dto.setUtilisationMax(p.getUtilisationMax());
        dto.setUtilisationCourante(p.getUtilisationCourante());
        dto.setActif(p.getActif());
        return dto;
    }
}
