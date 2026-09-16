package com.thehartford.identityservice.config;

import com.thehartford.identityservice.model.User;
import com.thehartford.identityservice.model.UserRole;
import com.thehartford.identityservice.model.UserStatus;
import com.thehartford.identityservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private static final String ADMIN_EMAIL    = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "123";

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeAdmin() {
        userRepository.findByEmail(ADMIN_EMAIL)
                .flatMap(existingUser -> {
                    log.info("Default admin user already exists.");
                    return Mono.<User>empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // userId is NOT set — MySQL AUTO_INCREMENT assigns it
                    User admin = User.builder()
                            .name("admin")
                            .email(ADMIN_EMAIL)
                            .password(passwordEncoder.encode(ADMIN_PASSWORD))
                            .role(UserRole.ADMIN)
                            .status(UserStatus.ACTIVE)
                            .build();
                    return userRepository.save(admin);
                }))
                .doOnSuccess(user -> log.info("Default admin initialization completed"))
                .doOnError(error -> log.error("Failed to initialize admin user", error))
                .subscribe();
    }
}
