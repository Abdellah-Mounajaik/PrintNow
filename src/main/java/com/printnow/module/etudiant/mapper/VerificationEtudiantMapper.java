package com.printnow.module.etudiant.mapper;

import com.printnow.module.etudiant.dto.VerificationEtudiantResponseDTO;
import com.printnow.module.etudiant.model.VerificationEtudiant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VerificationEtudiantMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "emailUtilisateur")
    @Mapping(expression = "java(v.getUser().getPrenom() + \" \" + v.getUser().getNom())", target = "nomUtilisateur")
    @Mapping(expression = "java(v.getCarteEtudiantePath() != null)", target = "carteEtudiantePresente")
    @Mapping(expression = "java(v.getCarteIdentitePath() != null)", target = "carteIdentitePresente")
    VerificationEtudiantResponseDTO toDto(VerificationEtudiant v);
}
