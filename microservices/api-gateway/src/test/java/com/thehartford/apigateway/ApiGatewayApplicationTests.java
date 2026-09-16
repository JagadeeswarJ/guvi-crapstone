package com.thehartford.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false",
        "app.jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437",
        "app.jwt.issuer=identity-service"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
