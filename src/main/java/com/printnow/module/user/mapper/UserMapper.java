package com.printnow.module.user.mapper;

import com.printnow.module.shop.dto.PartnerRegistrationRequest;
import com.printnow.module.user.dto.UserRequestDTO;
import com.printnow.module.user.dto.UserResponseDTO;
import com.printnow.module.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Conversion de l'entité vers la réponse (on mappe le nom du rôle)
    @Mapping(source = "role.nom", target = "roleNom")
    UserResponseDTO toResponse(User entity);

    // Conversion de la requête vers l'entité
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true) // Le rôle sera géré dans le service
    User toEntity(UserRequestDTO dto);

    // Mise à jour d'une entité existante sans créer d'objet intermédiaire
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateEntityFromDto(UserRequestDTO dto, @MappingTarget User entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true) // On le gère dans le service
    @Mapping(target = "motDePasse", ignore = true) // On l'encode dans le service
    @Mapping(target = "actif", constant = "true") // On force à "true" par défaut
    User toEntityFromPartnerRequest(PartnerRegistrationRequest request);
}