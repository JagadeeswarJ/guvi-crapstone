package com.thehartford.identityservice;

import com.thehartford.identityservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the Identity Service application context.
 *
 * <p>Uses an in-memory test profile that disables Eureka and R2DBC auto-connect
 * so the context can be loaded without a running database or Eureka server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // Disable Eureka registration during tests
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        // Provide a minimal R2DBC URL (won't actually connect in NONE mode)
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb",
        "spring.r2dbc.username=sa",
        "spring.r2dbc.password=",
        // Disable schema init
        "spring.sql.init.mode=never",
        // JWT config for tests
        "app.jwt.secret=testsecrettestsecrettestsecrettestsecretXXXXXXXX",
        "app.jwt.expiration-ms=3600000",
        "app.jwt.issuer=identity-service"
})
class IdentityServiceApplicationTests {

    @Autowired
    private JwtService jwtService;

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts successfully
        assertThat(jwtService).isNotNull();
    }
}
