// com/example/nexusa/Reviewer/Dto/ReviewerRegisterDTO.java
package com.example.nexusa.Reviewer.Dto;

import lombok.Data;

@Data
public class ReviewerRegisterDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String code;       // the secret code pre-inserted in reviewer_codes
}