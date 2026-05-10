package com.printnow.module.order.service;

import com.printnow.module.order.dto.CommandeRequestDTO;
import com.printnow.module.order.dto.CommandeResponseDTO;
import com.printnow.module.order.dto.LigneCommandeRequestDTO;
import com.printnow.module.order.enums.ModeRetrait;
import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.mapper.CommandeMapper;
import com.printnow.module.order.model.*;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.shop.model.Produit;
import com.printnow.module.shop.repository.ProduitRepository;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ProduitRepository produitRepository;
    private final CommandeMapper commandeMapper;

    /**
     * Crée une nouvelle commande avec calcul des prix, taxes et commissions
     */
    @Transactional
    public CommandeResponseDTO createCommande(CommandeRequestDTO request, User client) {
        Commande commande = new Commande();
        commande.setNumeroCommande(generateOrderNumber());
        commande.setStatut(StatutCommande.EN_ATTENTE_PAIEMENT);
        commande.setDateCreation(LocalDateTime.now());
        commande.setClient(client);
        
        // Gestion du mode de retrait
        if (request.getModeRetrait() != null) {
            commande.setModeRetrait(ModeRetrait.valueOf(request.getModeRetrait()));
        }

        // Gestion de l'option Express 2h
        boolean isExpress = request.getExpress2h() != null && request.getExpress2h();
        commande.setExpress2h(isExpress);

        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new RuntimeException("La commande doit contenir au moins un article.");
        }

        // On récupère l'imprimerie via le premier produit de la liste
        Produit premierProduit = produitRepository.findById(request.getLignes().get(0).getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        commande.setImprimerie(premierProduit.getImprimerie());

        BigDecimal totalHT = BigDecimal.ZERO;

        for (LigneCommandeRequestDTO item : request.getLignes()) {
            Produit produit = produitRepository.findById(item.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé ID: " + item.getProduitId()));
            
            // Conversion des prix Double (du modèle Produit) vers BigDecimal pour la précision
            BigDecimal prixBase = BigDecimal.valueOf(produit.getPrixBase() != null ? produit.getPrixBase() : 0.0);
            BigDecimal prixPage = BigDecimal.valueOf(produit.getPrixParPage() != null ? produit.getPrixParPage() : 0.0);
            
            // Calcul du prix unitaire : Prix de base + (Nombre de pages * Prix par page)
            BigDecimal prixU = prixBase;
            if (item.getNbPages() != null && item.getNbPages() > 0) {
                prixU = prixU.add(prixPage.multiply(BigDecimal.valueOf(item.getNbPages())));
            }

            LigneCommande ligne = LigneCommande.builder()
                    .produit(produit)
                    .quantite(item.getQuantite())
                    .nbPages(item.getNbPages())
                    .couleur(item.getCouleur())
                    .rectoVerso(item.getRectoVerso())
                    .prixUnitaire(prixU)
                    .prixTotal(prixU.multiply(BigDecimal.valueOf(item.getQuantite())))
                    .build();

            commande.addLigneCommande(ligne);
            totalHT = totalHT.add(ligne.getPrixTotal());
        }

        // APPLICATION DE LA MAJORATION +50% SI OPTION EXPRESS ACTIVÉE
        if (isExpress) {
            BigDecimal surcharge = totalHT.multiply(new BigDecimal("0.50"));
            totalHT = totalHT.add(surcharge);
        }

        // Calculs financiers finaux
        commande.setTotalHT(totalHT);
        commande.setTotalTVA(totalHT.multiply(new BigDecimal("0.20"))); // TVA fixe à 20%
        commande.setTotalTTC(commande.getTotalHT().add(commande.getTotalTVA()));
        
        // Commission PrintHub (10% du TTC)
        BigDecimal commission = commande.getTotalTTC().multiply(new BigDecimal("0.10"));
        commande.setCommissionPlateforme(commission);
        
        // Montant net pour l'imprimeur
        commande.setMontantVerseImprimerie(commande.getTotalTTC().subtract(commission));

        Commande savedCommande = commandeRepository.save(commande);
        
        // Retourne le DTO via le mapper
        return commandeMapper.toDto(savedCommande);
    }

    /**
     * Génère un numéro de commande unique (ex: CMD-20250510-A1B2C)
     */
    private String generateOrderNumber() {
        return "CMD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
               + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    /**
     * Récupère l'historique des commandes pour une imprimerie (Dashboard Pro)
     */
    public List<CommandeResponseDTO> getCommandesForImprimerie(Long imprimerieId) {
        return commandeRepository.findByImprimerie_IdOrderByDateCreationDesc(imprimerieId)
                .stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }
}