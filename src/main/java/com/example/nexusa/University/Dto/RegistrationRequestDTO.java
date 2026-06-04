package com.example.nexusa.University.Dto;

import com.example.nexusa.Model.Enums.Role;
import lombok.Data;

import java.util.UUID;

@Data
public class RegistrationRequestDTO {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Role role;
    private String adminCode;
    private UUID uniId;
}
