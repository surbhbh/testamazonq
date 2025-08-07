package com.transamerica.platform.amazonq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class TestControllerTest {

    @Test
    void testHealthEndpoint() {
        TestController controller = new TestController();
        String result = controller.health();
        assertEquals("OK", result);
    }

    @Test
    void testApiEndpoint() {
        TestController controller = new TestController();
        TestController.TestResponse response = controller.test();
        assertEquals("Amazon Q Test", response.getMessage());
        assertEquals("SUCCESS", response.getStatus());
    }
}