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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ImprimerieService {

    private final ImprimerieRepository imprimerieRepository;
    private final ShopMapper shopMapper;
    private final UserRepository userRepository;

    public ImprimerieResponseDTO createImprimerie(ImprimerieRequestDTO dto) {
        Imprimerie imprimerie = shopMapper.toEntity(dto);
        User gerant = userRepository.findById(dto.getIdGerant())
            .orElseThrow(() -> new RuntimeException("Gérant non trouvé"));
        imprimerie.setGerant(gerant);
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
        
        shopMapper.updateImprimerieFromDto(dto, imprimerie);
        return shopMapper.toResponse(imprimerieRepository.save(imprimerie));
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