package com.printnow.module.shop.service;

import com.printnow.module.shop.dto.HoraireOuvertureRequestDTO;
import com.printnow.module.shop.dto.HoraireOuvertureResponseDTO;
import com.printnow.module.shop.mapper.ShopMapper;
import com.printnow.module.shop.model.HoraireOuverture;
import com.printnow.module.shop.repository.HoraireOuvertureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HoraireOuvertureService {

    private final HoraireOuvertureRepository horaireRepository;
    private final ShopMapper shopMapper;

    public HoraireOuvertureResponseDTO updateHoraire(Long id, HoraireOuvertureRequestDTO dto) {
        HoraireOuverture horaire = horaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horaire non trouvé avec l'id : " + id));
        shopMapper.updateHoraireFromDto(dto, horaire);
        return shopMapper.toResponse(horaireRepository.save(horaire));
    }
}
