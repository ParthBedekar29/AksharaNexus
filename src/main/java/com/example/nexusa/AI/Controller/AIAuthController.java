package com.example.nexusa.AI.Controller;

import com.example.nexusa.AI.Config.AIJwtUtil;
import com.example.nexusa.Model.*;
import com.example.nexusa.Repository.*;
import com.example.nexusa.University.Service.EmailService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/ai/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIAuthController {

    private final UserRepository userRepository;
    private final PublicUserRepository publicUserRepository;
    private final PublicUserEmailVerificationTokenRepository verificationTokenRepository;
    private final PublicUserPasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AIJwtUtil aiJwtUtil;
    private final EmailService emailService;

    // ── Login ─────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        Optional<PublicUser> publicUser = publicUserRepository.findByEmail(req.getEmail());
        if (publicUser.isPresent()) {
            PublicUser u = publicUser.get();
            if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
            if (!u.isEmailVerified()) {
                return ResponseEntity.status(403).body("Please verify your email before logging in.");
            }
            String token = aiJwtUtil.generateToken(u.getEmail(), u.getRole().name());
            return ResponseEntity.ok(new LoginResponse(token, u.getFirstName(), u.getEmail(), u.getRole().name()));
        }

        Optional<User> internalUser = userRepository.findByEmail(req.getEmail());
        if (internalUser.isPresent()) {
            User u = internalUser.get();
            if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
            String token = aiJwtUtil.generateToken(u.getEmail(), u.getRole().name());
            return ResponseEntity.ok(new LoginResponse(token, u.getFirstName(), u.getEmail(), u.getRole().name()));
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }

    // ── Register ──────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (publicUserRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body("Email already registered");
        }

        PublicUser user = new PublicUser();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmailVerified(false);
        publicUserRepository.save(user);

        PublicUserEmailVerificationToken verificationToken = new PublicUserEmailVerificationToken();
        verificationToken.setToken(UUID.randomUUID());
        verificationToken.setPublicUser(user);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(verificationToken);

        emailService.sendPublicUserVerificationEmail(
                user.getEmail(), user.getFirstName(), verificationToken.getToken().toString());

        return ResponseEntity.status(201).body("Registration successful. Please check your email to verify your account.");
    }

    // ── Verify email ──────────────────────────────────────────────────────────
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        Optional<PublicUserEmailVerificationToken> opt =
                verificationTokenRepository.findByTokenAndUsedFalse(UUID.fromString(token));

        if (opt.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid or already used verification link.");
        }
        PublicUserEmailVerificationToken vToken = opt.get();
        if (vToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("Verification link has expired. Please register again.");
        }

        vToken.setUsed(true);
        verificationTokenRepository.save(vToken);

        PublicUser user = vToken.getPublicUser();
        user.setEmailVerified(true);
        publicUserRepository.save(user);

        return ResponseEntity.ok("Email verified successfully. You can now log in.");
    }

    // ── Forgot password ───────────────────────────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        Optional<PublicUser> opt = publicUserRepository.findByEmail(req.getEmail());
        if (opt.isEmpty()) {
            return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
        }

        PublicUser user = opt.get();

        PublicUserPasswordResetToken resetToken = new PublicUserPasswordResetToken();
        resetToken.setToken(UUID.randomUUID());
        resetToken.setPublicUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetTokenRepository.save(resetToken);

        emailService.sendPublicUserPasswordResetEmail(
                user.getEmail(), user.getFirstName(), resetToken.getToken().toString());

        return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
    }

    // ── Reset password ────────────────────────────────────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        Optional<PublicUserPasswordResetToken> opt =
                resetTokenRepository.findByTokenAndUsedFalse(UUID.fromString(req.getToken()));

        if (opt.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid or already used reset link.");
        }
        PublicUserPasswordResetToken rToken = opt.get();
        if (rToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("Reset link has expired. Please request a new one.");
        }

        rToken.setUsed(true);
        resetTokenRepository.save(rToken);

        PublicUser user = rToken.getPublicUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        publicUserRepository.save(user);

        return ResponseEntity.ok("Password reset successfully. You can now log in.");
    }
    // ── Resend verification ───────────────────────────────────────────────────
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ForgotPasswordRequest req) {
        Optional<PublicUser> opt = publicUserRepository.findByEmail(req.getEmail());
        if (opt.isEmpty() || opt.get().isEmailVerified()) {
            return ResponseEntity.ok("If that email is pending verification, a new link has been sent.");
        }

        PublicUser user = opt.get();

        // Invalidate old tokens
        verificationTokenRepository.findAllByPublicUser(user)
                .forEach(t -> { t.setUsed(true); verificationTokenRepository.save(t); });

        PublicUserEmailVerificationToken token = new PublicUserEmailVerificationToken();
        token.setToken(UUID.randomUUID());
        token.setPublicUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(token);

        emailService.sendPublicUserVerificationEmail(
                user.getEmail(), user.getFirstName(), token.getToken().toString());

        return ResponseEntity.ok("Verification email resent.");
    }
    // ── DTOs ──────────────────────────────────────────────────────────────────
    @Data public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data public static class RegisterRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String password;
    }

    @Data public static class ForgotPasswordRequest {
        private String email;
    }

    @Data public static class ResetPasswordRequest {
        private String token;
        private String newPassword;
    }

    @Data public static class LoginResponse {
        private final String token;
        private final String firstName;
        private final String email;
        private final String role;
    }
}