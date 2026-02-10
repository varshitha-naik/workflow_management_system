package com.example.workflow_management_system.runner;

import com.example.workflow_management_system.model.User;
import com.example.workflow_management_system.model.UserRole;
import com.example.workflow_management_system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class GlobalSuperAdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(GlobalSuperAdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public GlobalSuperAdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        verifyAndCreateGlobalSuperAdmin();
    }

    private void verifyAndCreateGlobalSuperAdmin() {
        try {
            logger.info("=== STARTING GLOBAL SUPER ADMIN RESET ===");

            String username = "global_superadmin";
            String email = "globaladmin@test.com";

            // 1. Delete Existing Global Super Admin by Username
            userRepository.findByUsername(username).ifPresent(user -> {
                logger.warn("Found existing user with username '{}'. DELETING...", username);
                userRepository.delete(user);
                logger.info("Deleted user with username '{}'.", username);
            });

            // 2. Delete Existing Global Super Admin by Email (if different user)
            userRepository.findByEmail(email).ifPresent(user -> {
                logger.warn("Found existing user with email '{}'. DELETING...", email);
                userRepository.delete(user);
                logger.info("Deleted user with email '{}'.", email);
            });

            // Double check ensure no old data remains
            if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
                logger.error("Failed to clean up existing Global Super Admin. Aborting creation to avoid duplicates.");
                return;
            }

            // 3. Create Fresh Global Super Admin
            logger.info("Creating FRESH Global Super Admin...");
            User globalAdmin = new User();
            globalAdmin.setUsername(username);
            globalAdmin.setEmail(email);
            globalAdmin.setPassword(passwordEncoder.encode("admin123"));
            globalAdmin.setRole(UserRole.GLOBAL_ADMIN);
            globalAdmin.setTenant(null); // IMPORTANT: No Tenant
            globalAdmin.setActive(true);

            userRepository.save(globalAdmin);

            logger.info("SUCCESS: Global Super Admin created.");
            logger.info("Username: {}", username);
            logger.info("Role: {}", UserRole.GLOBAL_ADMIN);
            logger.info("Tenant: null");
            logger.info("=== GLOBAL SUPER ADMIN RESET COMPLETE ===");

        } catch (Exception e) {
            logger.error("CRITICAL ERROR during Global Super Admin Seeding: ", e);
        }
    }
}
