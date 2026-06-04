package com.example.nexusa.AI.Controller;

import com.example.nexusa.AI.Config.AIJwtUtil;
import com.example.nexusa.Model.PublicUser;
import com.example.nexusa.Model.User;
import com.example.nexusa.Repository.PublicUserRepository;
import com.example.nexusa.Repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")  // open to Flutter mobile too
public class AIAuthController {

    private final UserRepository userRepository;
    private final PublicUserRepository publicUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AIJwtUtil aiJwtUtil;  // fixed casing

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        // Check public users first
        Optional<PublicUser> publicUser = publicUserRepository.findByEmail(req.getEmail());
        if (publicUser.isPresent()) {
            if (!passwordEncoder.matches(req.getPassword(), publicUser.get().getPassword())) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
            String token = aiJwtUtil.generateToken(
                    publicUser.get().getEmail(), publicUser.get().getRole().name());
            return ResponseEntity.ok(new LoginResponse(
                    token, publicUser.get().getFirstName(),
                    publicUser.get().getEmail(), publicUser.get().getRole().name()
            ));
        }

        // Fall back to internal users
        Optional<User> internalUser = userRepository.findByEmail(req.getEmail());
        if (internalUser.isPresent()) {
            if (!passwordEncoder.matches(req.getPassword(), internalUser.get().getPassword())) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
            String token = aiJwtUtil.generateToken(
                    internalUser.get().getEmail(), internalUser.get().getRole().name());
            return ResponseEntity.ok(new LoginResponse(
                    token, internalUser.get().getFirstName(),
                    internalUser.get().getEmail(), internalUser.get().getRole().name()
            ));
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }

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

        publicUserRepository.save(user);

        String token = aiJwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.status(201).body(new LoginResponse(
                token, user.getFirstName(), user.getEmail(), user.getRole().name()
        ));
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private final String token;
        private final String firstName;
        private final String email;
        private final String role;
    }
}