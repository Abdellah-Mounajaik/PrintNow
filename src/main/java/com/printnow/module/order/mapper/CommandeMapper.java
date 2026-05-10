package com.printnow.module.order.mapper;

import com.printnow.module.order.dto.CommandeRequestDTO;
import com.printnow.module.order.dto.CommandeResponseDTO;
import com.printnow.module.order.dto.LigneCommandeResponseDTO;
import com.printnow.module.order.model.Commande;
import com.printnow.module.order.model.LigneCommande;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class CommandeMapper {

    /**
     * Transforme une entité Commande en CommandeResponseDTO
     */
    // 👇 C'EST ICI QUE LA CORRECTION A ÉTÉ FAITE (CommandeResponseDTO au lieu de CommandeRequestDTO)
    public CommandeResponseDTO toDto(Commande commande) {
        if (commande == null) {
            return null;
        }

        CommandeResponseDTO dto = new CommandeResponseDTO();
        
        // Informations de base
        dto.setId(commande.getId());
        dto.setNumeroCommande(commande.getNumeroCommande());
        dto.setStatut(commande.getStatut() != null ? commande.getStatut().name() : null);
        dto.setModeRetrait(commande.getModeRetrait() != null ? commande.getModeRetrait().name() : null);
        dto.setDateCreation(commande.getDateCreation());
        
        // Mapping de l'option Express 2h
        dto.setExpress2h(commande.getExpress2h());

        // Montants financiers
        dto.setTotalHT(commande.getTotalHT());
        dto.setTotalTVA(commande.getTotalTVA());
        dto.setTotalTTC(commande.getTotalTTC());

        // Mapping de l'Utilisateur (Client)
        if (commande.getClient() != null) {
            dto.setNomClient(commande.getClient().getPrenom() + " " + commande.getClient().getNom());
        }

        // Mapping de l'Imprimerie
        if (commande.getImprimerie() != null) {
            dto.setNomImprimerie(commande.getImprimerie().getNom());
        }

        // Transformation de la liste des lignes de commande
        if (commande.getLignes() != null) {
            dto.setLignes(commande.getLignes().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setLignes(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Transforme une entité LigneCommande en LigneCommandeResponseDTO
     */
    public LigneCommandeResponseDTO toDto(LigneCommande ligne) {
        if (ligne == null) {
            return null;
        }

        LigneCommandeResponseDTO dto = new LigneCommandeResponseDTO();
        
        dto.setId(ligne.getId());
        dto.setQuantite(ligne.getQuantite());
        dto.setNbPages(ligne.getNbPages());
        dto.setCouleur(ligne.getCouleur());
        dto.setRectoVerso(ligne.getRectoVerso());
        dto.setPrixUnitaire(ligne.getPrixUnitaire());
        dto.setPrixTotal(ligne.getPrixTotal());

        // Mapping du Produit lié
        if (ligne.getProduit() != null) {
            dto.setProduitId(ligne.getProduit().getId());
            
            // Construction du libellé produit à partir des Enums (Type + Format)
            // Exemple : "DOCUMENT A4" ou "FLYER A5"
            String typeStr = ligne.getProduit().getTypeProduit() != null 
                    ? ligne.getProduit().getTypeProduit().name() : "PRODUIT";
            String formatStr = ligne.getProduit().getFormatImpression() != null 
                    ? ligne.getProduit().getFormatImpression().name() : "";
            
            dto.setNomProduit(typeStr + " " + formatStr);
        }

        return dto;
    }
}