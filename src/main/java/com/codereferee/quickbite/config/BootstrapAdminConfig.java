package com.codereferee.quickbite.config;

import com.codereferee.quickbite.user.UserAccount;
import com.codereferee.quickbite.user.UserAccountRepository;
import com.codereferee.quickbite.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminConfig.class);

    @Bean
    ApplicationRunner bootstrapAdmin(
            UserAccountRepository users,
            PasswordEncoder encoder,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String email,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password
    ) {
        return args -> {
            if (email.isBlank() || password.isBlank() || users.existsByEmailIgnoreCase(email)) {
                return;
            }
            UserAccount admin = new UserAccount();
            admin.setEmail(email.trim().toLowerCase());
            admin.setDisplayName("QuickBite Admin");
            admin.setPasswordHash(encoder.encode(password));
            admin.setRole(UserRole.ADMIN);
            users.save(admin);
            log.warn("Bootstrapped admin account email={}; remove BOOTSTRAP_ADMIN_PASSWORD after first use", admin.getEmail());
        };
    }
}
