package com.example.authserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SwaggerConfigTest {

    @Test
    void contextLoads() {
        // This test ensures that the Spring application context loads successfully
        // with Swagger configuration
    }
}
