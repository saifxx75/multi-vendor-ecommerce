package com.marketplace.config;

import com.marketplace.entity.Category;
import com.marketplace.entity.Role;
import com.marketplace.entity.User;
import com.marketplace.repository.CategoryRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates the first admin (admins cannot self-register) and a few starter categories.
 * Safe to run on every start: it only inserts what is missing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        String email = adminEmail.trim().toLowerCase();
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(new User(adminName, email, passwordEncoder.encode(adminPassword), Role.ADMIN));
            log.info("Seeded admin account: {}", email);
        }

        if (categoryRepository.count() == 0) {
            List<String> names = List.of("Electronics", "Fashion", "Home & Kitchen", "Books");
            for (String name : names) {
                Category category = new Category();
                category.setName(name);
                categoryRepository.save(category);
            }
            log.info("Seeded {} starter categories", names.size());
        }
    }
}
