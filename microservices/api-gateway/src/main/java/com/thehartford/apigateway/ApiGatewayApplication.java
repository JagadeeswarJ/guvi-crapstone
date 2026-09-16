package com.thehartford.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * LeaseBond API Gateway Microservice
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Registers with Eureka as API-GATEWAY</li>
 *   <li>Discovers downstream microservices via Eureka</li>
 *   <li>Routes public /api/v1/** requests to internal microservice endpoints</li>
 *   <li>Performs manual JWT authentication for protected endpoints</li>
 *   <li>Forwards authenticated requests with Authorization header intact</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
