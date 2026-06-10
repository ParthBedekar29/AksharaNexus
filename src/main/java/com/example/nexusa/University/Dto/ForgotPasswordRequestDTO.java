package com.example.nexusa.University.Dto;
import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {
    private String email;
    private String adminCode; // null for VIEWER, required for ADMIN
}