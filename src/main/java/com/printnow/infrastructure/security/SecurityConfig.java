package com.printnow.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permet de gérer la sécurité au niveau des méthodes (ex: @PreAuthorize)
@RequiredArgsConstructor // Injecte automatiquement les dépendances `final`
public class SecurityConfig {

    // On injecte les classes que nous avons créées précédemment
    private final CustomUserDetailsService userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final AuthTokenFilter authTokenFilter;

    // Configure la façon dont on va chercher l'utilisateur et vérifier le mot de passe
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Expose l'AuthenticationManager pour l'utiliser dans le AuthController
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. On garde votre configuration CORS
            .cors(Customizer.withDefaults()) 
            
            // 2. On désactive CSRF (indispensable pour les API REST avec JWT)
            .csrf(AbstractHttpConfigurer::disable) 
            
            // 3. On gère les erreurs 401 Unauthorized de manière propre
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            
            // 4. On passe en mode "Stateless" : le serveur ne garde pas de session, tout est dans le token
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 5. Configuration des accès aux routes
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll() // Redispatch d'erreur : évite que 400/403/409 soient transformés en 401
                .requestMatchers("/api/auth/**").permitAll() // L'authentification est publique
                .requestMatchers("/api/imprimeries/**").permitAll() // Le catalogue est public
                .requestMatchers("/api/produits/**").permitAll()
                .requestMatchers("/api/horaires/**").permitAll()
                .requestMatchers("/api/partners/register").permitAll() // L'inscription des partenaires est publique
                .requestMatchers("/api/uploads/**").permitAll() // Upload du logo pendant l'inscription (avant authentification)
                .requestMatchers("/uploads/**").permitAll() // Fichiers publics servis statiquement (logos)
                .requestMatchers("/api/payments/**").permitAll()
                .requestMatchers("/api/contact").permitAll() // Le formulaire de contact est public
                .requestMatchers("/api/chat").permitAll() // L'assistant de la FAQ est public
                .requestMatchers("/api/promos/**").authenticated()
                .requestMatchers("/api/partners/**").permitAll()
                .requestMatchers("/api/fichiers-pdf/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/avis/imprimerie/**").permitAll() // Lecture des avis publique
                .anyRequest().authenticated() // TOUT le reste nécessite un token JWT valide
            );

        // 6. On attache notre fournisseur d'authentification
        http.authenticationProvider(authenticationProvider());
        
        // 7. ÉTAPE CLÉ : On place notre filtre JWT AVANT le filtre de connexion standard de Spring
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}