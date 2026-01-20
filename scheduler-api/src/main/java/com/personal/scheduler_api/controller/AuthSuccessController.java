package com.personal.scheduler_api.controller;

import java.io.IOException;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.scheduler_api.model.GoogleToken;
import com.personal.scheduler_api.repository.GoogleTokenRepository;
import com.personal.scheduler_api.service.GoogleCalendarService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthSuccessController {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final GoogleTokenRepository googleTokenRepository;
    private final GoogleCalendarService googleCalendarService;

    @GetMapping("/success")
    public void handleLoginSuccess(OAuth2AuthenticationToken authentication, HttpServletResponse response)
            throws IOException {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName());

        if (client != null) {
            String email = authentication.getPrincipal().getAttribute("email");

            GoogleToken token = googleTokenRepository.findById(email).orElse(new GoogleToken());
            token.setEmail(email);
            token.setAccessToken(client.getAccessToken().getTokenValue());
            token.setExpiresAt(client.getAccessToken().getExpiresAt());

            // PENTING: Refresh token hanya dikirim Google saat login pertama kali (consent
            // screen)
            // Kita cek null agar tidak menimpa refresh token lama dengan null
            if (client.getRefreshToken() != null) {
                token.setRefreshToken(client.getRefreshToken().getTokenValue());
            }

            googleTokenRepository.save(token);

            try {
                // FIX: Gunakan Domain Production untuk Webhook (Bukan Ngrok/Localhost)
                String productionUrl = "https://aawzyy.my.id";
                googleCalendarService.watchCalendar(token.getAccessToken(), productionUrl, email);
            } catch (Exception e) {
                // Log warning saja agar login tetap lanjut meskipun webhook gagal
                System.err.println("Webhook warning: " + e.getMessage());
            }
        }

        // === PASANG JEJAK DI SINI ===
        System.out.println("\n==================================================");
        System.out.println(">>> DEBUG: MENGIRIM REDIRECT KE HTTPS DASHBOARD <<<");
        System.out.println("==================================================\n");

        // --- FINAL REDIRECT (Production Only) ---
        // Memaksa user ke halaman Dashboard HTTPS
        response.sendRedirect("https://aawzyy.my.id/dashboard");
    }
}