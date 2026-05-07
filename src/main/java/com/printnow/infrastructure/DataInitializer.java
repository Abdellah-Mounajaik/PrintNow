package com.printnow.infrastructure;

import com.printnow.module.user.model.Role;
import com.printnow.module.user.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByNom("CLIENT").isEmpty()) {
                Role clientRole = new Role();
                clientRole.setNom("CLIENT"); 
                roleRepository.save(clientRole);
                System.out.println("✅ Rôle CLIENT créé avec succès en base de données.");
            }

            if (roleRepository.findByNom("ADMIN").isEmpty()) {
                Role adminRole = new Role();
                adminRole.setNom("ADMIN"); 
                roleRepository.save(adminRole);
                System.out.println("✅ Rôle ADMIN créé avec succès en base de données.");
            }
            if (roleRepository.findByNom("IMPRIMERIE").isEmpty()) {
                Role imprimerieRole = new Role();
                imprimerieRole.setNom("IMPRIMERIE"); 
                roleRepository.save(imprimerieRole);
                System.out.println("✅ Rôle IMPRIMERIE créé avec succès.");
            }
        };
    }
}