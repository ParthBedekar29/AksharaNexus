// com/example/nexusa/Reviewer/Controller/ReviewerAuthController.java
package com.example.nexusa.Reviewer.Controller;

import com.example.nexusa.Reviewer.Dto.ReviewerLoginDTO;
import com.example.nexusa.Reviewer.Dto.ReviewerRegisterDTO;
import com.example.nexusa.Reviewer.Service.ReviewerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviewer/auth")
public class ReviewerAuthController {

    private final ReviewerService reviewerService;

    public ReviewerAuthController(ReviewerService reviewerService) {
        this.reviewerService = reviewerService;
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