package com.printnow.infrastructure.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation interactive de l'API (Swagger UI sur /swagger-ui.html,
 * spécification OpenAPI brute sur /v3/api-docs). Déclare le schéma
 * d'authentification JWT pour que le bouton "Authorize" de Swagger UI
 * fonctionne sur les routes protégées.
 */
@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI printNowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API PrintNow")
                        .description("API REST de la plateforme d'impression en ligne PrintNow "
                                + "(commandes, imprimeries partenaires, correction orthographique, "
                                + "génération de designs par IA, vérification du statut étudiant).")
                        .version("v1")
                        .contact(new Contact().name("PrintNow")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                                .name(JWT_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton obtenu via POST /api/auth/login, à coller ici sans le préfixe \"Bearer \".")));
    }
}
