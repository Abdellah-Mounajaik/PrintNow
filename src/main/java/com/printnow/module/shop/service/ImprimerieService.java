package com.printnow.module.shop.service;

import com.printnow.module.shop.dto.ImprimerieRequestDTO;
import com.printnow.module.shop.dto.ImprimerieResponseDTO;
import com.printnow.module.shop.mapper.ShopMapper;
import com.printnow.module.shop.model.Imprimerie;
import com.printnow.module.shop.repository.ImprimerieRepository;
import com.printnow.module.user.model.User;
import com.printnow.module.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ImprimerieService {

    private final ImprimerieRepository imprimerieRepository;
    private final ShopMapper shopMapper;
    private final UserRepository userRepository;
    private final GeocodingService geocodingService;
    private final SlugImprimerieService slugService;

    public ImprimerieResponseDTO createImprimerie(ImprimerieRequestDTO dto) {
        Imprimerie imprimerie = shopMapper.toEntity(dto);
        User gerant = userRepository.findById(dto.getIdGerant())
            .orElseThrow(() -> new RuntimeException("Gérant non trouvé"));
        imprimerie.setGerant(gerant);
        geocodeAndSetCoordonnees(imprimerie);
        slugService.attribuer(imprimerie);
        return shopMapper.toResponse(imprimerieRepository.save(imprimerie));
    }

    @Transactional(readOnly = true)
    public List<ImprimerieResponseDTO> getAllActiveImprimeries() {
        return imprimerieRepository.findAllByActifTrue().stream()
                .map(shopMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ImprimerieResponseDTO getImprimerieById(Long id) {
        Imprimerie imprimerie = imprimerieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imprimerie non trouvée avec l'id : " + id));
        return shopMapper.toResponse(imprimerie);
    }

    /**
     * Récupère l'imprimerie depuis l'adresse lisible de sa fiche.
     *
     * Les adresses partagées avant la mise en place du slug portent encore un
     * numéro : on les accepte toujours, pour ne pas les briser.
     */
    @Transactional(readOnly = true)
    public ImprimerieResponseDTO getImprimerieBySlug(String slug) {
        return imprimerieRepository.findBySlug(slug)
                .map(shopMapper::toResponse)
                .orElseGet(() -> {
                    if (!slug.matches("\\d+")) {
                        throw new RuntimeException("Imprimerie non trouvée : " + slug);
                    }
                    return getImprimerieById(Long.parseLong(slug));
                });
    }

    /** Récupère l'imprimerie gérée par un utilisateur (dashboard imprimeur). */
    @Transactional(readOnly = true)
    public ImprimerieResponseDTO getImprimerieByGerantId(Long idGerant) {
        Imprimerie imprimerie = imprimerieRepository.findByGerantId(idGerant).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Aucune imprimerie associée à ce compte."));
        return shopMapper.toResponse(imprimerie);
    }

    public ImprimerieResponseDTO updateImprimerie(Long id, ImprimerieRequestDTO dto) {
        Imprimerie imprimerie = imprimerieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imprimerie non trouvée avec l'id : " + id));

        // On ne re-géocode que si l'adresse a réellement changé (ou si on n'a jamais réussi
        // à la localiser), pour éviter un appel réseau à chaque simple modification d'option.
        boolean adresseChangee = !Objects.equals(imprimerie.getAdresse(), dto.getAdresse())
                || !Objects.equals(imprimerie.getVille(), dto.getVille());
        boolean coordonneesManquantes = imprimerie.getLatitude() == null;

        shopMapper.updateImprimerieFromDto(dto, imprimerie);

        if (adresseChangee || coordonneesManquantes) {
            geocodeAndSetCoordonnees(imprimerie);
        }

        // L'adresse de la fiche suit le nom : elle n'est refaite que s'il change.
        slugService.attribuer(imprimerie);

        return shopMapper.toResponse(imprimerieRepository.save(imprimerie));
    }

    /** Convertit l'adresse de l'imprimerie en coordonnées GPS (fail open si introuvable). */
    private void geocodeAndSetCoordonnees(Imprimerie imprimerie) {
        if (imprimerie.getAdresse() == null || imprimerie.getAdresse().isBlank()) {
            return;
        }
        String ville = imprimerie.getVille();
        boolean villeUtile = ville != null && !ville.isBlank() && !"Non spécifiée".equalsIgnoreCase(ville);

        String query = imprimerie.getAdresse()
                + (villeUtile ? ", " + ville : "")
                + ", Belgique";

        geocodingService.geocode(query).ifPresent(coords -> {
            imprimerie.setLatitude(coords[0]);
            imprimerie.setLongitude(coords[1]);
        });
    }

    public void deleteImprimerie(Long id) {
        if (!imprimerieRepository.existsById(id)) {
            throw new RuntimeException("Impossible de supprimer : Imprimerie non trouvée");
        }
        // Au lieu de supprimer physiquement, on pourrait juste la rendre inactive (Soft Delete)
        Imprimerie imprimerie = imprimerieRepository.findById(id).get();
        imprimerie.setActif(false);
        imprimerieRepository.save(imprimerie);
    }
}