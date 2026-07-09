package com.assessment.orderservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Welcoming index controller to prevent 404 errors at the root context path (http://localhost:8080/).
 */
@RestController
public class IndexController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getIndex() {
        return ResponseEntity.ok(Map.of(
                "service", "Order Management Service",
                "status", "UP",
                "apiDocs", "/api/orders",
                "message", "Welcome! The Order Service is running successfully. Use /api/orders to interact with the API."
        ));
    }
}
