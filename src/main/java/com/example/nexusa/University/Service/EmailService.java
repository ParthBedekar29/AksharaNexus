package com.example.nexusa.University.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;
    private static final String FRONTEND_URL = "https://contribute-aksharanexus.netlify.app";
    private static final String REVIEWER_FRONTEND_URL = "https://reviewer-aksharanexus.netlify.app";

    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String verifyUrl = FRONTEND_URL + "/verify.html?token=" + token;
        String html = """
        <div style="font-family: Inter, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
            <h2 style="color: #1a1a1a;">Verify your email</h2>
            <p style="color: #444;">Hi %s, thanks for registering on AksharaNexus.</p>
            <p style="color: #444;">Click the button below to verify your email address. This link expires in 24 hours.</p>
            <a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#1a1a1a;color:#fff;text-decoration:none;border-radius:6px;">Verify Email</a>
            <p style="margin-top:24px;color:#999;font-size:13px;">If you didn't register, ignore this email.</p>
        </div>
    """.formatted(firstName, verifyUrl);
        sendEmail(toEmail, "Verify your AksharaNexus account", html);
    }

    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String resetUrl = FRONTEND_URL + "/reset-password.html?token=" + token;
        String html = """
        <div style="font-family: Inter, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
            <h2 style="color: #1a1a1a;">Reset your password</h2>
            <p style="color: #444;">Hi %s, we received a request to reset your AksharaNexus password.</p>
            <p style="color: #444;">Click the button below to choose a new password. This link expires in 1 hour.</p>
            <a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#1a1a1a;color:#fff;text-decoration:none;border-radius:6px;">Reset Password</a>
            <p style="margin-top:24px;color:#999;font-size:13px;">If you didn't request this, ignore this email — your password won't change.</p>
        </div>
    """.formatted(firstName, resetUrl);
        sendEmail(toEmail, "Reset your AksharaNexus password", html);
    }

    public void sendReviewerVerificationEmail(String toEmail, String firstName, String token) {
        String verifyUrl = REVIEWER_FRONTEND_URL + "/verify.html?token=" + token;
        String html = """
        <div style="font-family: Inter, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
            <h2 style="color: #1a1a1a;">Verify your email</h2>
            <p style="color: #444;">Hi %s, thanks for registering as an AksharaNexus Reviewer.</p>
            <p style="color: #444;">Click the button below to verify your email address. This link expires in 24 hours.</p>
            <a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#1a1a1a;color:#fff;text-decoration:none;border-radius:6px;">Verify Email</a>
            <p style="margin-top:24px;color:#999;font-size:13px;">If you didn't register, ignore this email.</p>
        </div>
    """.formatted(firstName, verifyUrl);
        sendEmail(toEmail, "Verify your AksharaNexus Reviewer account", html);
    }

    public void sendReviewerPasswordResetEmail(String toEmail, String firstName, String token) {
        String resetUrl = REVIEWER_FRONTEND_URL + "/reset-password.html?token=" + token;
        String html = """
        <div style="font-family: Inter, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
            <h2 style="color: #1a1a1a;">Reset your password</h2>
            <p style="color: #444;">Hi %s, we received a request to reset your AksharaNexus Reviewer password.</p>
            <p style="color: #444;">Click the button below to choose a new password. This link expires in 1 hour.</p>
            <a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#1a1a1a;color:#fff;text-decoration:none;border-radius:6px;">Reset Password</a>
            <p style="margin-top:24px;color:#999;font-size:13px;">If you didn't request this, ignore this email — your password won't change.</p>
        </div>
    """.formatted(firstName, resetUrl);
        sendEmail(toEmail, "Reset your AksharaNexus Reviewer password", html);
    }

    private void sendEmail(String toEmail, String subject, String html) {
        String sanitizedHtml = html.replace("\"", "\\\"").replace("\n", "").replace("    ", "");
        String body = """
        {
            "from": "AksharaNexus <noreply@aksharanexus.site>",
            "to": ["%s"],
            "subject": "%s",
            "html": "%s"
        }
    """.formatted(toEmail, subject, sanitizedHtml);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}