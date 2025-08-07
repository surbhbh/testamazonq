package com.transamerica.platform.amazonq;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test REST controller for Amazon Q demonstration
 */
@RestController
public class TestController {
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
    
    @GetMapping("/api/test")
    public TestResponse test() {
        return new TestResponse("Amazon Q Test", "SUCCESS");
    }
    
    public static class TestResponse {
        private String message;
        private String status;
        
        public TestResponse(String message, String status) {
            this.message = message;
            this.status = status;
        }
        
        public String getMessage() { return message; }
        public String getStatus() { return status; }
    }
}