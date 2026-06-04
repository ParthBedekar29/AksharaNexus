package com.example.nexusa.University.Service;

import com.example.nexusa.University.Dto.LoginRequestDTO;
import com.example.nexusa.University.Dto.RegistrationRequestDTO;
import com.example.nexusa.Model.AdminCodes;
import com.example.nexusa.Model.Enums.Role;
import com.example.nexusa.Model.University;
import com.example.nexusa.Model.UniversityDomain;
import com.example.nexusa.Model.User;
import com.example.nexusa.Repository.AdminCodeRepository;
import com.example.nexusa.Repository.UniversityRepository;
import com.example.nexusa.Repository.UserRepository;
import com.example.nexusa.University.Utility.JwtUtil;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final AdminCodeRepository adminCodeRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, UniversityRepository universityRepository, AdminCodeRepository adminCodeRepository, JwtUtil util, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.universityRepository = universityRepository;
        this.adminCodeRepository = adminCodeRepository;
        this.jwtUtil = util;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegistrationRequestDTO registrationRequestDTO) {

        String entered_mail = registrationRequestDTO.getEmail();
        String firstName = registrationRequestDTO.getFirstName();
        if (firstName == null) {
            throw new IllegalArgumentException("First Name is Required");
        }
        if (!Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", entered_mail)) {
            throw new IllegalArgumentException("Email Format Invalid");
        } else {
            UUID uniId = registrationRequestDTO.getUniId();

            Optional<University> university = universityRepository.findById(uniId);

            if (university.isEmpty()) {
                throw new IllegalArgumentException("No university exists by given ID");
            }
            List<UniversityDomain> domains = university.get().getDomains();

            String domain = entered_mail.substring(entered_mail.lastIndexOf("@") + 1);

            boolean found = false;
            for (UniversityDomain udomian : domains) {
                if (udomian.getDomain().equals(domain)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                String password = registrationRequestDTO.getPassword();
                if (!Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", password)) {
                    throw new IllegalArgumentException("Password Must Contain at least 8 characters, 1 Uppercase Letter, Number and Symbol");
                } else {
                    Role role = registrationRequestDTO.getRole();

                    if (role == Role.ADMIN) {
                        Optional<AdminCodes> adminCode = adminCodeRepository.findAdminCodesByEmail(entered_mail);
                        if (adminCode.isEmpty()) {
                            throw new IllegalArgumentException("Entered Secret Key does not exist!");
                        } else {
                            String code = adminCode.get().getCode();
                            if (!registrationRequestDTO.getAdminCode().equals(code)) {
                                throw new IllegalArgumentException("Entered Secret Key is Incorrect");
                            } else if (adminCode.get().isUsed()) {
                                throw new IllegalArgumentException("Account with this ID exists Kindly Log In");
                            } else {
                                adminCode.get().setUsed(true);
                                adminCodeRepository.save(adminCode.get());
                                String hashed = passwordEncoder.encode(password);

                                User user = buildUser(registrationRequestDTO, hashed, university.get(), role);
                                userRepository.save(user);
                                return jwtUtil.generateToken(user);
                            }
                        }
                    } else if (role == Role.VIEWER) {
                        String hashed = passwordEncoder.encode(password);
                        User user = buildUser(registrationRequestDTO, hashed, university.get(), role);
                        userRepository.save(user);
                        return jwtUtil.generateToken(user);
                    } else {
                        throw new IllegalArgumentException("Invalid role specified");
                    }
                }
            } else {
                throw new IllegalArgumentException("Email domain does not match selected university");
            }

        }

    }

    private User buildUser(RegistrationRequestDTO dto, String hashedPassword, University university, Role role) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setRole(role);
        user.setPassword(hashedPassword);
        user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        user.setUniID(university);
        return user;
    }

    public String login(LoginRequestDTO loginRequestDTO) {
        String entered_mail = loginRequestDTO.getEmail();
        if (!Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", entered_mail)) {
            throw new IllegalArgumentException("Email Format Invalid");
        } else {
            Optional<User> existing=userRepository.findUserByEmail(entered_mail);
            if (existing.isEmpty()) {
                throw new IllegalArgumentException("No Account Exists with provided email");
            }
            String password = loginRequestDTO.getPassword();

            if(!passwordEncoder.matches(password,existing.get().getPassword())){
                throw new IllegalArgumentException("Password Incorrect");
            }
            return jwtUtil.generateToken(existing.get());
        }

    }
}