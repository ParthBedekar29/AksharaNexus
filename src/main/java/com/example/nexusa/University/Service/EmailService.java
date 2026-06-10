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

    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String verifyUrl = FRONTEND_URL + "/verify.html?token=" + token;

        String html = """
            <div style="font-family: Inter, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
                <h2 style="color: #1a1a1a;">Verify your email</h2>
                <p style="color: #444;">Hi %s, thanks for registering on AksharaNexus.</p>
                <p style="color: #444;">Click the button below to verify your email address. This link expires in 24 hours.</p>
                <a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#1a1a1a;color:#fff;text-decoration:none;border-radius:6px;">
                    Verify Email
                </a>
                <p style="margin-top:24px;color:#999;font-size:13px;">If you didn't register, ignore this email.</p>
            </div>
        """.formatted(firstName, verifyUrl);

        String body = """
            {
                "from": "AksharaNexus <onboarding@resend.dev>",
                "to": ["%s"],
                "subject": "Verify your AksharaNexus account",
                "html": "%s"
            }
        """.formatted(toEmail, html.replace("\"", "\\\"").replace("\n", "").replace("    ", ""));

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
            System.err.println("Failed to send verification email: " + e.getMessage());
        }
    }
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String resetUrl = FRONTEND_URL + "/reset-password.html?token=" + token;
        String html = """
        <div style="font-family: Inter, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px;">
            <h2 style="color: #1a1a1a;">Reset your password</h2>
            <p style="color: #444;">Hi %s, we received a request to reset your AksharaNexus password.</p>
            <p style="color: #444;">Click the button below to choose a new password. This link expires in 1 hour.</p>
            <a href="%s" style="display:inline-block;margin-top:16px;padding:12px 24px;background:#1a1a1a;color:#fff;text-decoration:none;border-radius:6px;">
                Reset Password
            </a>
            <p style="margin-top:24px;color:#999;font-size:13px;">If you didn't request this, ignore this email — your password won't change.</p>
        </div>
    """.formatted(firstName, resetUrl);

        String body = """
        {
            "from": "AksharaNexus <onboarding@resend.dev>",
            "to": ["%s"],
            "subject": "Reset your AksharaNexus password",
            "html": "%s"
        }
    """.formatted(toEmail, html.replace("\"", "\\\"").replace("\n", "").replace("    ", ""));

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
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }
}