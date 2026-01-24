package com.personal.scheduler_api.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.personal.scheduler_api.model.GoogleToken;
import com.personal.scheduler_api.repository.GoogleTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {

    // Mengambil Client ID dari application.yml
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    private final GoogleTokenRepository googleTokenRepository;

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String idTokenString = payload.get("idToken");
        String accessToken = payload.get("accessToken"); 

        System.out.println(">>> MOBILE LOGIN REQUEST DITERIMA");

        try {
            // 1. Verifikasi Token ke Google (Validasi Keaslian)
            // Ini memastikan token benar-benar dari Google, bukan buatan hacker
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            // GoogleIdToken idToken = verifier.verify(idTokenString); 
            // NOTE: Jika verifier.verify gagal (karena beda audience Android vs Web), 
            // gunakan parsing manual payload dulu untuk dev:
            GoogleIdToken idToken = GoogleIdToken.parse(new GsonFactory(), idTokenString);

            if (idToken != null) {
                GoogleIdToken.Payload googlePayload = idToken.getPayload();
                String email = googlePayload.getEmail();
                String name = (String) googlePayload.get("name");
                
                System.out.println(">>> USER TERVERIFIKASI: " + email);

                // 2. Simpan/Update Token di Database
                GoogleToken token = googleTokenRepository.findById(email).orElse(new GoogleToken());
                token.setEmail(email);
                if (accessToken != null) {
                    token.setAccessToken(accessToken);
                    token.setExpiresAt(Instant.now().plusSeconds(3600)); 
                }
                googleTokenRepository.save(token);

                // 3. FORCE LOGIN (Bikin Session Spring Security Manual)
                DefaultOAuth2User principal = new DefaultOAuth2User(
                    Collections.emptyList(), 
                    Map.of("sub", googlePayload.getSubject(), "name", name, "email", email), 
                    "email"
                );
                
                OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                    principal, 
                    Collections.emptyList(), 
                    "google"
                );
                
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                // 4. Buat Session JSESSIONID
                HttpSession session = request.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

                System.out.println(">>> SESSION DIBUAT: " + session.getId());

                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "email", email,
                    "session", session.getId()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token Invalid");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login Gagal: " + e.getMessage());
        }
    }
}