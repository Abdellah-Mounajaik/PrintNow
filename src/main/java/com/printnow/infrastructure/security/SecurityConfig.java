package com.printnow.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. On dit à Spring Security d'utiliser ton fichier WebConfig pour le CORS
            .cors(Customizer.withDefaults()) 
            
            // 2. On désactive temporairement la protection CSRF (indispensable quand on utilise React en local)
            .csrf(csrf -> csrf.disable()) 
            
            // 3. On autorise toutes les requêtes pour le moment (tu bloqueras plus tard)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() 
            );

        return http.build();
    }
}
