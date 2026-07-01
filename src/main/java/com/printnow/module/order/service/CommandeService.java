package com.printnow.module.order.service;

import com.printnow.module.order.dto.CommandeRequestDTO;
import com.printnow.module.order.dto.CommandeResponseDTO;
import com.printnow.module.order.dto.LigneCommandeRequestDTO;
import com.printnow.module.order.enums.ModeRetrait;
import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.enums.StatutLivraison;
import com.printnow.module.order.mapper.AdresseLivraisonMapper;
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
    private final AdresseLivraisonMapper adresseLivraisonMapper; 

    /**
     * Crée une nouvelle commande avec calcul des prix, taxes, livraison et commissions
     */
    @Transactional
    public CommandeResponseDTO createCommande(CommandeRequestDTO request, User client) {
        Commande commande = new Commande();
        commande.setNumeroCommande(generateOrderNumber());
        commande.setStatut(StatutCommande.EN_ATTENTE_PAIEMENT);
        commande.setDateCreation(LocalDateTime.now());
        commande.setClient(client);
        
        // 1. Gestion du mode de retrait et de la livraison
        boolean isLivraison = false;
        if (request.getModeRetrait() != null) {
            ModeRetrait mode = ModeRetrait.valueOf(request.getModeRetrait());
            commande.setModeRetrait(mode);
            // Vérifie si le mode correspond à une livraison à domicile
            if (mode == ModeRetrait.LIVRAISON ) { 
                isLivraison = true;
            }
        }

        // 2. Création de l'adresse et de la livraison si nécessaire
        if (isLivraison && request.getAdresseLivraison() != null) {
            // Création de l'entité AdresseLivraison via le Mapper ! (1 seule ligne)
            AdresseLivraison adresse = adresseLivraisonMapper.toEntity(request.getAdresseLivraison());
            commande.setAdresseLivraison(adresse); // Enregistré automatiquement grâce au CascadeType.ALL

            // Initialisation du suivi de Livraison
            Livraison livraison = Livraison.builder()
                    .statutLivraison(StatutLivraison.EN_PREPARATION)
                    .commande(commande) // Liaison bidirectionnelle
                    .build();
            commande.setLivraison(livraison);
        }

        // 3. Gestion de l'option Express 2h
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

        // 4. Boucle sur chaque fichier/produit commandé
        for (LigneCommandeRequestDTO item : request.getLignes()) {
            Produit produit = produitRepository.findById(item.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé ID: " + item.getProduitId()));
            
            BigDecimal prixBase = BigDecimal.valueOf(produit.getPrixBase() != null ? produit.getPrixBase() : 0.0);
            BigDecimal prixPage = BigDecimal.valueOf(produit.getPrixParPage() != null ? produit.getPrixParPage() : 0.0);
            
            // Calcul du prix unitaire : (Prix de base + Prix par page) * Nombre de pages
            BigDecimal nbPages = BigDecimal.valueOf(item.getNbPages() != null && item.getNbPages() > 0 ? item.getNbPages() : 1);
            BigDecimal prixU = prixBase.add(prixPage).multiply(nbPages);

            // Ajout du prix de la Reliure
            if (item.getReliure() != null && !item.getReliure().equals("AUCUNE") && produit.getPrixParTypeReliure() != null) {
                Object prixRel = produit.getPrixParTypeReliure().get(item.getReliure());
                if (prixRel != null) {
                    prixU = prixU.add(new BigDecimal(prixRel.toString()));
                }
            }
            
            // Ajout du prix de la Finition / Plastification
            if (item.getFinition() != null && !item.getFinition().equals("AUCUNE") && produit.getPrixParTypePlastification() != null) {
                Object prixFin = produit.getPrixParTypePlastification().get(item.getFinition());
                if (prixFin != null) {
                    prixU = prixU.add(new BigDecimal(prixFin.toString()));
                }
            }

            // Construction de la ligne de commande
            LigneCommande ligne = LigneCommande.builder()
                    .produit(produit)
                    .quantite(item.getQuantite())
                    .nbPages(item.getNbPages())
                    .couleur(item.getCouleur())
                    .rectoVerso(item.getRectoVerso())
                    .reliure(item.getReliure()) 
                    .finition(item.getFinition())
                    .prixUnitaire(prixU)
                    .prixTotal(prixU.multiply(BigDecimal.valueOf(item.getQuantite())))
                    .build();

            commande.addLigneCommande(ligne);
            totalHT = totalHT.add(ligne.getPrixTotal());
        }

        // 5. Application de la majoration +50% si option Express activée
        if (isExpress) {
            BigDecimal surcharge = totalHT.multiply(new BigDecimal("0.50"));
            totalHT = totalHT.add(surcharge);
        }

        // 6. Ajout des frais de livraison au Total HT
        if (isLivraison) {
            totalHT = totalHT.add(new BigDecimal("4.99")); // Frais de port fixes pour le moment
        }

        // 7. Calculs financiers finaux
        commande.setTotalHT(totalHT);
        commande.setTotalTVA(totalHT.multiply(new BigDecimal("0.20"))); // TVA fixe à 20%
        commande.setTotalTTC(commande.getTotalHT().add(commande.getTotalTVA()));
        
        // Commission PrintHub (10% du TTC)
        BigDecimal commission = commande.getTotalTTC().multiply(new BigDecimal("0.10"));
        commande.setCommissionPlateforme(commission);
        
        // Montant net pour l'imprimeur
        commande.setMontantVerseImprimerie(commande.getTotalTTC().subtract(commission));

        // 8. Sauvegarde en base (sauvegarde la commande, ses lignes, l'adresse et le suivi de livraison !)
        Commande savedCommande = commandeRepository.save(commande);
        
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

    /**
     * Met à jour le statut d'une commande (ex: PAYEE → EN_COURS_IMPRESSION → PRETE)
     */
    @Transactional
    public CommandeResponseDTO updateStatut(Long id, String nouveauStatut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + id));
        commande.setStatut(StatutCommande.valueOf(nouveauStatut));
        return commandeMapper.toDto(commandeRepository.save(commande));
    }

    /**
     * Récupère l'historique des commandes d'un client (Dashboard Client)
     */
    public List<CommandeResponseDTO> getCommandesForClient(Long clientId) {
        return commandeRepository.findByClient_IdOrderByDateCreationDesc(clientId)
                .stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }
}