package com.printnow.module.avis.service;

import com.printnow.module.avis.dto.AvisImprimerieDTO;
import com.printnow.module.avis.dto.AvisRequestDTO;
import com.printnow.module.avis.dto.AvisResponseDTO;
import com.printnow.module.avis.mapper.AvisMapper;
import com.printnow.module.avis.model.Avis;
import com.printnow.module.avis.repository.AvisRepository;
import com.printnow.module.order.enums.StatutCommande;
import com.printnow.module.order.repository.CommandeRepository;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.repository.ImprimerieRepository;
import com.printnow.module.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvisService {

    private final AvisRepository avisRepository;
    private final ImprimerieRepository imprimerieRepository;
    private final CommandeRepository commandeRepository;
    private final AvisMapper avisMapper;
    private final ModerationService moderationService;

    /**
     * Un client laisse un avis. Conditions :
     *  - l'imprimerie existe
     *  - la note est entre 1 et 5
     *  - le client a au moins une commande LIVREE chez cette imprimerie
     *  - le client n'a pas déjà laissé un avis (contrainte unique)
     */
    @Transactional
    public AvisResponseDTO creerAvis(User client, AvisRequestDTO request) {
        if (request.getNote() == null || request.getNote() < 1 || request.getNote() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note doit être comprise entre 1 et 5.");
        }

        Imprimerie imprimerie = imprimerieRepository.findById(request.getImprimerieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imprimerie introuvable."));

        boolean aCommandeLivree = commandeRepository.existsByClient_IdAndImprimerie_IdAndStatut(
                client.getId(), imprimerie.getId(), StatutCommande.LIVREE);
        if (!aCommandeLivree) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez laisser un avis qu'après une commande livrée chez cette imprimerie.");
        }

        if (avisRepository.existsByUser_IdAndImprimerie_Id(client.getId(), imprimerie.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà laissé un avis pour cette imprimerie.");
        }

        // Modération IA du commentaire : on rejette la soumission si un propos injurieux est détecté
        if (moderationService.estInapproprie(request.getCommentaire())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Votre commentaire contient un langage inapproprié. Merci de le reformuler.");
        }

        Avis avis = new Avis();
        avis.setUser(client);
        avis.setImprimerie(imprimerie);
        avis.setNote(request.getNote());
        avis.setCommentaire(request.getCommentaire());
        Avis saved = avisRepository.save(avis);

        recalculerStats(imprimerie);

        return avisMapper.toDto(saved);
    }

    /**
     * Vue complète des avis d'une imprimerie. clientId peut être null (visiteur anonyme).
     */
    public AvisImprimerieDTO getAvisImprimerie(Long imprimerieId, Long clientId) {
        List<AvisResponseDTO> avis = avisRepository.findByImprimerie_IdOrderByDateCreationDesc(imprimerieId)
                .stream()
                .map(avisMapper::toDto)
                .collect(Collectors.toList());

        Double moyenne = avisRepository.moyenneNoteByImprimerie(imprimerieId);
        long nombre = avisRepository.countByImprimerie_Id(imprimerieId);

        AvisImprimerieDTO dto = new AvisImprimerieDTO();
        dto.setNoteMoyenne(moyenne);
        dto.setNombreAvis(nombre);
        dto.setAvis(avis);

        if (clientId != null) {
            boolean dejaNote = avisRepository.existsByUser_IdAndImprimerie_Id(clientId, imprimerieId);
            boolean aCommandeLivree = commandeRepository.existsByClient_IdAndImprimerie_IdAndStatut(
                    clientId, imprimerieId, StatutCommande.LIVREE);
            dto.setDejaNote(dejaNote);
            dto.setPeutNoter(aCommandeLivree && !dejaNote);
        }

        return dto;
    }

    /** Recalcule et stocke la note moyenne + le nombre d'avis sur l'imprimerie. */
    private void recalculerStats(Imprimerie imprimerie) {
        Double moyenne = avisRepository.moyenneNoteByImprimerie(imprimerie.getId());
        long nombre = avisRepository.countByImprimerie_Id(imprimerie.getId());
        imprimerie.setNoteMoyenne(moyenne != null ? Math.round(moyenne * 10.0) / 10.0 : null);
        imprimerie.setNombreAvis((int) nombre);
        imprimerieRepository.save(imprimerie);
    }
}
