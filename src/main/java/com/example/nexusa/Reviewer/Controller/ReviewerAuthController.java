// com/example/nexusa/Reviewer/Controller/ReviewerAuthController.java
package com.example.nexusa.Reviewer.Controller;

import com.example.nexusa.Reviewer.Dto.ReviewerLoginDTO;
import com.example.nexusa.Reviewer.Dto.ReviewerRegisterDTO;
import com.example.nexusa.Reviewer.Service.ReviewerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reviewer/auth")
public class ReviewerAuthController {

    private final ReviewerService reviewerService;

    public ReviewerAuthController(ReviewerService reviewerService) {
        this.reviewerService = reviewerService;
    }
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        try {
            reviewerService.verifyEmail(token);
            return ResponseEntity.ok("Email verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            reviewerService.forgotPassword(body.get("email"));
            return ResponseEntity.ok("Password reset link sent. Please check your email.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        try {
            reviewerService.resetPassword(body.get("token"), body.get("newPassword"));
            return ResponseEntity.ok("Password reset successfully. You can now sign in.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody ReviewerRegisterDTO dto) {
        try {
            reviewerService.register(dto);
            return ResponseEntity.ok("Reviewer registered successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody ReviewerLoginDTO dto) {
        try {
            String token = reviewerService.login(dto);
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}