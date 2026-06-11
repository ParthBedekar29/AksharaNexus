package com.example.nexusa.AI.Controller;

import com.example.nexusa.AI.Config.AIJwtUtil;
import com.example.nexusa.Model.PublicUser;
import com.example.nexusa.Repository.PublicUserEmailVerificationTokenRepository;
import com.example.nexusa.Repository.PublicUserPasswordResetTokenRepository;
import com.example.nexusa.Repository.PublicUserRepository;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/ai/account")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIAccountController {

    private final PublicUserRepository publicUserRepository;
    private final PublicUserEmailVerificationTokenRepository verificationTokenRepository;
    private final PublicUserPasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AIJwtUtil aiJwtUtil;

    // ── Get profile ───────────────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getProfile(Principal principal) {
        return publicUserRepository.findByEmail(principal.getName())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(new ProfileResponse(
                        u.getFirstName(),
                        u.getLastName(),
                        u.getEmail(),
                        u.getCreatedAt().toLocalDate().toString()
                )))
                .orElse(ResponseEntity.status(404).body("User not found"));
    }

    // ── Change password ───────────────────────────────────────────────────────
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req,
                                            Principal principal) {
        Optional<PublicUser> opt = publicUserRepository.findByEmail(principal.getName());
        if (opt.isEmpty()) return ResponseEntity.status(404).body("User not found");

        PublicUser user = opt.get();
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body("Current password is incorrect.");
        }
        if (req.getNewPassword().length() < 6) {
            return ResponseEntity.status(400).body("New password must be at least 6 characters.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        publicUserRepository.save(user);
        return ResponseEntity.ok("Password updated successfully.");
    }

    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<?> deleteAccount(@RequestBody DeleteAccountRequest req,
                                           Principal principal) {
        Optional<PublicUser> opt = publicUserRepository.findByEmail(principal.getName());
        if (opt.isEmpty()) return ResponseEntity.status(404).body("User not found");

        PublicUser user = opt.get();
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body("Incorrect password.");
        }

        verificationTokenRepository.deleteAllByPublicUser(user);
        verificationTokenRepository.flush();

        resetTokenRepository.deleteAllByPublicUser(user);
        resetTokenRepository.flush();

        publicUserRepository.delete(user);
        return ResponseEntity.ok("Account deleted.");
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────
    @Data public static class ProfileResponse {
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String memberSince;
    }

    @Data public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }

    @Data public static class DeleteAccountRequest {
        private String password;
    }
}