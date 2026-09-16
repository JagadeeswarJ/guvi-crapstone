package com.thehartford.identityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * LeaseBond Identity Service
 *
 * <p>Provides:
 * <ul>
 *   <li>User registration and authentication</li>
 *   <li>JWT token generation and validation</li>
 *   <li>Role-based user management (PROPERTY_OWNER, UNDERWRITER, CLAIMS_OFFICER, ADMIN)</li>
 * </ul>
 *
 * <p>Registered as a Eureka client on port 8761.
 * Exposes WebFlux functional routes (no @RestController).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
