package com.printnow.module.avis.mapper;

import com.printnow.module.avis.dto.AvisResponseDTO;
import com.printnow.module.avis.model.Avis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AvisMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(expression = "java(a.getUser().getPrenom() + \" \" + a.getUser().getNom())", target = "nomClient")
    AvisResponseDTO toDto(Avis a);
}
