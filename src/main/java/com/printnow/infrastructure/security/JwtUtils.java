package com.printnow.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Fabrique et vérifie les jetons de connexion.
 *
 * Le secret de signature vient de l'environnement (JWT_SECRET) et n'est jamais
 * écrit dans le code : qui le connaît peut forger un jeton pour n'importe quel
 * compte, administration comprise, sans avoir à s'authentifier. Un secret
 * versionné dans le dépôt annule donc toutes les autres protections du site.
 */
@Component
public class JwtUtils {

    /** HS256 exige une clé d'au moins 256 bits ; en dessous, la signature est cassable. */
    private static final int LONGUEUR_MINIMALE = 32;

    private final Key cle;
    private final long dureeDeValidite;

    public JwtUtils(@Value("${printnow.jwt.secret}") String secret,
                    @Value("${printnow.jwt.expiration-ms}") long dureeDeValidite) {
        refuserUnSecretTropFaible(secret);
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.dureeDeValidite = dureeDeValidite;
    }

    /**
     * Arrête le démarrage plutôt que de servir avec une clé devinable.
     *
     * Le défaut, ici, ne se voit pas à l'usage : l'application fonctionne
     * normalement et se laisse simplement usurper. Mieux vaut ne pas démarrer.
     */
    private void refuserUnSecretTropFaible(String secret) {
        if (secret == null || secret.strip().length() < LONGUEUR_MINIMALE) {
            throw new IllegalStateException(
                    "La variable d'environnement JWT_SECRET est absente ou trop courte : "
                            + LONGUEUR_MINIMALE + " caractères au minimum sont requis pour signer les jetons.");
        }
    }

    public String generateJwtToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + dureeDeValidite))
                .signWith(cle, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(cle).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(cle).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Token JWT invalide : " + e.getMessage());
        }
        return false;
    }
}
