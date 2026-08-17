package com.printnow.module.order.mapper;

import com.printnow.module.order.dto.CommandeRequestDTO;
import com.printnow.module.order.dto.CommandeResponseDTO;
import com.printnow.module.order.dto.FichierPDFResponseDTO;
import com.printnow.module.order.dto.LigneCommandeResponseDTO;
import com.printnow.module.order.model.Commande;
import com.printnow.module.order.model.FichierPDF;
import com.printnow.module.order.model.LigneCommande;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommandeMapper {

    private final AdresseLivraisonMapper adresseLivraisonMapper;

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
        // Comptés à part : la vérification orthographique et les designs IA sont des
        // services de la plateforme, hors du total facturé pour l'impression.
        dto.setMontantCorrections(commande.getMontantCorrections());
        dto.setMontantGenerations(commande.getMontantGenerations());

        // Mapping de l'Utilisateur (Client)
        if (commande.getClient() != null) {
            dto.setNomClient(commande.getClient().getPrenom() + " " + commande.getClient().getNom());
        }

        // Mapping de l'Imprimerie
        if (commande.getImprimerie() != null) {
            dto.setNomImprimerie(commande.getImprimerie().getNom());
        }

        // Mapping du numéro de suivi (si livraison)
        if (commande.getLivraison() != null) {
            dto.setNumeroSuivi(commande.getLivraison().getNumeroSuivi());
        }

        // Mapping de l'adresse de livraison (si livraison à domicile)
        if (commande.getAdresseLivraison() != null) {
            dto.setAdresseLivraison(adresseLivraisonMapper.toDto(commande.getAdresseLivraison()));
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
        dto.setReliure(ligne.getReliure());
        dto.setFinition(ligne.getFinition());
        dto.setPrixUnitaire(ligne.getPrixUnitaire());
        dto.setPrixTotal(ligne.getPrixTotal());

        // Mapping du Produit lié
        if (ligne.getProduit() != null) {
            dto.setProduitId(ligne.getProduit().getId());

            String typeStr = ligne.getProduit().getTypeProduit() != null
                    ? ligne.getProduit().getTypeProduit().name() : "PRODUIT";
            String formatStr = ligne.getProduit().getFormatImpression() != null
                    ? ligne.getProduit().getFormatImpression().name() : "";

            dto.setNomProduit(typeStr + " " + formatStr);
        }

        if (ligne.getFichiers() != null) {
            dto.setFichiers(ligne.getFichiers().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public FichierPDFResponseDTO toDto(FichierPDF fichier) {
        if (fichier == null) return null;
        FichierPDFResponseDTO dto = new FichierPDFResponseDTO();
        dto.setId(fichier.getId());
        dto.setNomFichier(fichier.getNomFichier());
        dto.setTailleOctets(fichier.getTailleOctets());
        dto.setNbPagesDetectees(fichier.getNbPagesDetectees());
        dto.setDateUpload(fichier.getDateUpload());
        return dto;
    }
}