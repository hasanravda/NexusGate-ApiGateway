package com.nexusgate.auth.config;

import com.nexusgate.auth.model.User;
import com.nexusgate.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {
    
    @Bean
    public CommandLineRunner loadData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default admin user if not exists
            if (userRepository.findByEmail("admin@demo.com").isEmpty()) {
                User admin = new User();
                admin.setEmail("admin@demo.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                admin.setIsActive(true);
                userRepository.save(admin);
                System.out.println("✅ Default admin user created: admin@demo.com / admin123");
            }
        };
    }
}
