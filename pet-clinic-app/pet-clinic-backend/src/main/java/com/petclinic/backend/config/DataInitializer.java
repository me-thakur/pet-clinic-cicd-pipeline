package com.petclinic.backend.config;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;
import com.petclinic.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data Initializer for creating default users
 * 
 * Creates default admin user for testing and development
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
    }

    private void initializeDefaultUsers() {
        logger.info("Starting user initialization...");
        
        // Create admin user if it doesn't exist
        boolean adminExists = userRepository.existsByUsername("admin");
        logger.info("Admin user exists: {}", adminExists);
        if (!adminExists) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@petclinic.com");
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setAccountNonExpired(true);
            admin.setAccountNonLocked(true);
            admin.setCredentialsNonExpired(true);

            userRepository.save(admin);
            logger.info("Created default admin user: admin/admin123");
        }

        // Create vet users
        boolean vet1Exists = userRepository.existsByUsername("vet1");
        logger.info("Vet1 user exists: {}", vet1Exists);
        if (!vet1Exists) {
            User vet1 = new User();
            vet1.setUsername("vet1");
            vet1.setPassword(passwordEncoder.encode("vet123"));
            vet1.setEmail("vet1@petclinic.com");
            vet1.setFirstName("Dr. Vet");
            vet1.setLastName("One");
            vet1.setRole(Role.VET);
            vet1.setEnabled(true);
            vet1.setAccountNonExpired(true);
            vet1.setAccountNonLocked(true);
            vet1.setCredentialsNonExpired(true);

            userRepository.save(vet1);
            logger.info("Created default vet user: vet1/vet123");
        }

        boolean vet2Exists = userRepository.existsByUsername("vet2");
        logger.info("Vet2 user exists: {}", vet2Exists);
        if (!vet2Exists) {
            User vet2 = new User();
            vet2.setUsername("vet2");
            vet2.setPassword(passwordEncoder.encode("vet123"));
            vet2.setEmail("vet2@petclinic.com");
            vet2.setFirstName("Dr. Vet");
            vet2.setLastName("Two");
            vet2.setRole(Role.VET);
            vet2.setEnabled(true);
            vet2.setAccountNonExpired(true);
            vet2.setAccountNonLocked(true);
            vet2.setCredentialsNonExpired(true);

            userRepository.save(vet2);
            logger.info("Created default vet user: vet2/vet123");
        }

        // Create staff users
        boolean staff1Exists = userRepository.existsByUsername("staff1");
        logger.info("Staff1 user exists: {}", staff1Exists);
        if (!staff1Exists) {
            User staff1 = new User();
            staff1.setUsername("staff1");
            staff1.setPassword(passwordEncoder.encode("staff123"));
            staff1.setEmail("staff1@petclinic.com");
            staff1.setFirstName("Staff");
            staff1.setLastName("One");
            staff1.setRole(Role.STAFF);
            staff1.setEnabled(true);
            staff1.setAccountNonExpired(true);
            staff1.setAccountNonLocked(true);
            staff1.setCredentialsNonExpired(true);

            userRepository.save(staff1);
            logger.info("Created default staff user: staff1/staff123");
        }

        boolean staff2Exists = userRepository.existsByUsername("staff2");
        logger.info("Staff2 user exists: {}", staff2Exists);
        if (!staff2Exists) {
            User staff2 = new User();
            staff2.setUsername("staff2");
            staff2.setPassword(passwordEncoder.encode("staff123"));
            staff2.setEmail("staff2@petclinic.com");
            staff2.setFirstName("Staff");
            staff2.setLastName("Two");
            staff2.setRole(Role.STAFF);
            staff2.setEnabled(true);
            staff2.setAccountNonExpired(true);
            staff2.setAccountNonLocked(true);
            staff2.setCredentialsNonExpired(true);

            userRepository.save(staff2);
            logger.info("Created default staff user: staff2/staff123");
        }

        // Create receptionist user for backward compatibility
        boolean receptionistExists = userRepository.existsByUsername("receptionist1");
        logger.info("Receptionist1 user exists: {}", receptionistExists);
        if (!receptionistExists) {
            User receptionist = new User();
            receptionist.setUsername("receptionist1");
            receptionist.setPassword(passwordEncoder.encode("staff123"));
            receptionist.setEmail("receptionist1@petclinic.com");
            receptionist.setFirstName("Reception");
            receptionist.setLastName("Staff");
            receptionist.setRole(Role.STAFF);
            receptionist.setEnabled(true);
            receptionist.setAccountNonExpired(true);
            receptionist.setAccountNonLocked(true);
            receptionist.setCredentialsNonExpired(true);

            userRepository.save(receptionist);
            logger.info("Created default receptionist user: receptionist1/staff123");
        }

        logger.info("Data initialization completed");
    }
}