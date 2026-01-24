package com.personal.scheduler_api.controller;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.scheduler_api.model.GoogleToken;
import com.personal.scheduler_api.repository.GoogleTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {

    private final GoogleTokenRepository googleTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // Alat bedah JSON bawaan Spring

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        System.out.println(">>> [1] REQUEST MASUK (METODE MANUAL)");
        
        String idTokenString = payload.get("idToken");
        String accessToken = payload.get("accessToken"); 

        if (idTokenString == null || idTokenString.isEmpty()) {
            return ResponseEntity.badRequest().body("Token Kosong");
        }

        try {
            // --- BEDAH TOKEN SECARA MANUAL (TANPA LIBRARY GOOGLE) ---
            // Token JWT itu isinya: Header.Payload.Signature
            // Kita cuma butuh Payload (bagian tengah)
            
            String[] parts = idTokenString.split("\\.");
            if (parts.length < 2) {
                 System.out.println(">>> [ERROR] Token tidak punya 3 bagian");
                 return ResponseEntity.badRequest().body("Format Token Salah");
            }

            // Decode Base64
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            System.out.println(">>> [2] Isi Token: " + payloadJson);

            // Ambil Email & Nama pakai Jackson
            JsonNode jsonNode = objectMapper.readTree(payloadJson);
            
            // Cek apakah email ada (untuk memastikan ini token login)
            if (!jsonNode.has("email")) {
                 System.out.println(">>> [ERROR] Tidak ada email di token");
                 return ResponseEntity.badRequest().body("Token tidak valid (No Email)");
            }

            String email = jsonNode.get("email").asText();
            String name = jsonNode.has("name") ? jsonNode.get("name").asText() : "Mobile User";
            
            System.out.println(">>> [3] User Terbaca: " + email);

            // --- PROSES LOGIN (SIMPAN KE DB) ---
            GoogleToken token = googleTokenRepository.findById(email).orElse(new GoogleToken());
            token.setEmail(email);
            if (accessToken != null) {
                token.setAccessToken(accessToken);
                token.setExpiresAt(Instant.now().plusSeconds(3600)); 
            }
            googleTokenRepository.save(token);

            // --- BIKIN SESSION ---
            DefaultOAuth2User principal = new DefaultOAuth2User(
                Collections.emptyList(), 
                Map.of("sub", "mobile", "name", name, "email", email), 
                "email"
            );
            
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                principal, 
                Collections.emptyList(), 
                "google"
            );
            
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            System.out.println(">>> [4] LOGIN SUKSES! Session: " + session.getId());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "email", email,
                "session", session.getId()
            ));

        } catch (Exception e) {
            System.out.println(">>> [FATAL] Gagal Decode Manual: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}