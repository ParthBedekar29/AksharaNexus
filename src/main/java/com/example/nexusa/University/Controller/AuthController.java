package com.example.nexusa.University.Controller;

import com.example.nexusa.University.Dto.ForgotPasswordRequestDTO;
import com.example.nexusa.University.Dto.LoginRequestDTO;
import com.example.nexusa.University.Dto.RegistrationRequestDTO;
import com.example.nexusa.University.Dto.ResetPasswordRequestDTO;
import com.example.nexusa.University.Service.AuthService;
import com.example.nexusa.University.Service.UniversityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
public class AuthController {

    private final AuthService authService;
    private final UniversityService universityService;

    public AuthController(AuthService authService, UniversityService universityService) {
        this.authService = authService;
        this.universityService = universityService;
    }
    @GetMapping("/auth/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        try {
            String message = authService.verifyEmail(token);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@RequestBody RegistrationRequestDTO registrationRequestDTO) {
        try {
            authService.register(registrationRequestDTO);
            return ResponseEntity.ok("Registration successful. Please check your email to verify your account.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/auth/universities")
    public ResponseEntity<List<Map<String, String>>> getUniversities() {
        List<Map<String, String>> result = universityService.getAllUniversities();

        return ResponseEntity.ok(result);
    }
    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO loginRequestDTO){
        try{
            String token=authService.login(loginRequestDTO);

            if(token!=null){
                return ResponseEntity.ok(token);
            }
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.badRequest().body("Login failed");
    }
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequestDTO dto) {
        try {
            authService.forgotPassword(dto);
            return ResponseEntity.ok("Password reset link sent. Please check your email.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequestDTO dto) {
        try {
            authService.resetPassword(dto);
            return ResponseEntity.ok("Password reset successfully. You can now sign in.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
