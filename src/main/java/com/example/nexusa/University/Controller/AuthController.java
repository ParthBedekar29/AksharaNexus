package com.example.nexusa.University.Controller;

import com.example.nexusa.University.Dto.LoginRequestDTO;
import com.example.nexusa.University.Dto.RegistrationRequestDTO;
import com.example.nexusa.University.Service.AuthService;
import com.example.nexusa.University.Service.UniversityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@RequestBody RegistrationRequestDTO registrationRequestDTO){
        try{
            String token=authService.register(registrationRequestDTO);

            if(token!=null){
                return ResponseEntity.ok(token);
            }
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.badRequest().body("Registration failed");
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

}
