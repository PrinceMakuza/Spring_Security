package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Security Test", description = "Endpoints for testing security features")
public class TestController {

    @PostMapping("/csrf-demo")
    @Operation(summary = "Endpoint for CSRF demonstration")
    public ResponseEntity<ApiResponse<String>> csrfDemo(@RequestParam String data) {
        return ResponseEntity.ok(ApiResponse.success("CSRF Demo received data: " + data, null));
    }

    @GetMapping("/role-admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin-only test endpoint")
    public ResponseEntity<ApiResponse<String>> adminOnly() {
        return ResponseEntity.ok(ApiResponse.success("Access granted to ADMIN", null));
    }

    @GetMapping("/role-customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Customer-only test endpoint")
    public ResponseEntity<ApiResponse<String>> customerOnly() {
        return ResponseEntity.ok(ApiResponse.success("Access granted to CUSTOMER", null));
    }
}
