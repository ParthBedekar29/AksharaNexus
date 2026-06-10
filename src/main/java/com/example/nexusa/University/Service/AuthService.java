package com.example.nexusa.University.Service;

import com.example.nexusa.Model.*;
import com.example.nexusa.Repository.*;
import com.example.nexusa.University.Dto.ForgotPasswordRequestDTO;
import com.example.nexusa.University.Dto.LoginRequestDTO;
import com.example.nexusa.University.Dto.RegistrationRequestDTO;
import com.example.nexusa.Model.Enums.Role;
import com.example.nexusa.University.Dto.ResetPasswordRequestDTO;
import com.example.nexusa.University.Utility.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
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
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthService(UserRepository userRepository,
                       UniversityRepository universityRepository,
                       AdminCodeRepository adminCodeRepository,
                       JwtUtil util,
                       PasswordEncoder passwordEncoder,
                       EmailVerificationTokenRepository tokenRepository,
                       EmailService emailService, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.universityRepository = universityRepository;
        this.adminCodeRepository = adminCodeRepository;
        this.jwtUtil = util;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public String register(RegistrationRequestDTO registrationRequestDTO) {
        String entered_mail = registrationRequestDTO.getEmail();
        String firstName = registrationRequestDTO.getFirstName();

        if (firstName == null) throw new IllegalArgumentException("First Name is Required");

        if (!Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", entered_mail)) {
            throw new IllegalArgumentException("Email Format Invalid");
        }

        UUID uniId = registrationRequestDTO.getUniId();
        Optional<University> university = universityRepository.findById(uniId);
        if (university.isEmpty()) throw new IllegalArgumentException("No university exists by given ID");

        List<UniversityDomain> domains = university.get().getDomains();
        String domain = entered_mail.substring(entered_mail.lastIndexOf("@") + 1);

        boolean found = false;
        for (UniversityDomain udomain : domains) {
            if (udomain.getDomain().equals(domain)) { found = true; break; }
        }
        if (!found) throw new IllegalArgumentException("Email domain does not match selected university");

        String password = registrationRequestDTO.getPassword();
        if (!Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", password)) {
            throw new IllegalArgumentException("Password Must Contain at least 8 characters, 1 Uppercase Letter, Number and Symbol");
        }

        Role role = registrationRequestDTO.getRole();

        if (role == Role.ADMIN) {
            Optional<AdminCodes> adminCode = adminCodeRepository.findAdminCodesByEmail(entered_mail);
            if (adminCode.isEmpty()) throw new IllegalArgumentException("Entered Secret Key does not exist!");

            String code = adminCode.get().getCode();
            if (!registrationRequestDTO.getAdminCode().equals(code))
                throw new IllegalArgumentException("Entered Secret Key is Incorrect");
            if (adminCode.get().isUsed())
                throw new IllegalArgumentException("Account with this ID exists Kindly Log In");

            adminCode.get().setUsed(true);
            adminCodeRepository.save(adminCode.get());
            String hashed = passwordEncoder.encode(password);
            User user = buildUser(registrationRequestDTO, hashed, university.get(), role);
            saveUserAndSendVerification(user);
            return null;

        } else if (role == Role.VIEWER) {
            String hashed = passwordEncoder.encode(password);
            User user = buildUser(registrationRequestDTO, hashed, university.get(), role);
            saveUserAndSendVerification(user);
            return null;

        } else {
            throw new IllegalArgumentException("Invalid role specified");
        }
    }

    private void saveUserAndSendVerification(User user) {
        userRepository.save(user);

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser_UserId(user.getUserId());

        // Create new verification token
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        tokenRepository.save(token);

        // Send verification email
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFirstName(),
                token.getToken().toString()
        );
    }
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO dto) {
        String email = dto.getEmail();

        if (!Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", email)) {
            throw new IllegalArgumentException("Email format invalid");
        }

        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account exists with this email"));

        // Only university-level roles can use this flow
        if (user.getRole() != Role.ADMIN &&
                user.getRole() != Role.EDITOR &&
                user.getRole() != Role.VIEWER) {
            throw new IllegalArgumentException("Password reset is not available for this account type");
        }

        // ADMIN must verify their secret key first
        if (user.getRole() == Role.ADMIN) {
            String adminCode = dto.getAdminCode();
            if (adminCode == null || adminCode.isBlank()) {
                throw new IllegalArgumentException("Admin accounts require the secret key to reset password");
            }
            AdminCodes code = adminCodeRepository.findAdminCodesByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("No admin code found for this email"));
            if (!adminCode.equals(code.getCode())) {
                throw new IllegalArgumentException("Secret key is incorrect");
            }
        }

        // Clear any existing reset tokens for this user
        passwordResetTokenRepository.deleteByUser_UserId(user.getUserId());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(UUID.randomUUID());
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getFirstName(),
                resetToken.getToken().toString()
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO dto) {
        UUID tokenUUID;
        try {
            tokenUUID = UUID.fromString(dto.getToken());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid reset token format");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(tokenUUID)
                .orElseThrow(() -> new IllegalArgumentException("Token not found or already used"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset link has expired. Please request a new one.");
        }

        if (!Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", dto.getNewPassword())) {
            throw new IllegalArgumentException("Password must contain at least 8 characters, 1 uppercase letter, number and symbol");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
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
        }

        Optional<User> existing = userRepository.findUserByEmail(entered_mail);
        if (existing.isEmpty()) throw new IllegalArgumentException("No Account Exists with provided email");

        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), existing.get().getPassword())) {
            throw new IllegalArgumentException("Password Incorrect");
        }

        if (!existing.get().isEmailVerified()) {
            throw new IllegalArgumentException("Please verify your email before logging in");
        }

        return jwtUtil.generateToken(existing.get());
    }
    public String verifyEmail(String rawToken) {
        UUID tokenUUID;
        try {
            tokenUUID = UUID.fromString(rawToken);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid verification token format");
        }

        Optional<EmailVerificationToken> found = tokenRepository.findByToken(tokenUUID);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Token not found or already used");
        }

        EmailVerificationToken verificationToken = found.get();

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Verification token has expired. Please register again.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken); // one-time use

        return "Email verified successfully";
    }
}