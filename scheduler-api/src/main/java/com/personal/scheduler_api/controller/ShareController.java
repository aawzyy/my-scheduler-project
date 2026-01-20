package com.personal.scheduler_api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.personal.scheduler_api.model.ShareToken;
import com.personal.scheduler_api.service.ShareService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping
    public ResponseEntity<?> generateLink(@RequestParam String type) {
        // --- CCTV LOGS ---
        System.out.println(">>> 1. Request Masuk ke ShareController!");
        System.out.println(">>> 2. Menerima Type: " + type);

        try {
            ShareToken token = shareService.createShareToken(type);
            
            System.out.println(">>> 3. Token Berhasil Dibuat: " + token.getId());
            
            // --- FIX: GUNAKAN DOMAIN PRODUCTION (HTTPS) ---
            // Bukan localhost:5173 lagi
            String link = "https://aawzyy.my.id/meet/" + token.getId();
            
            return ResponseEntity.ok(Map.of(
                "token", token.getId(),
                "type", token.getType(),
                "expiresAt", token.getExpiresAt(),
                "shareLink", link
            ));
        } catch (Exception e) {
            System.err.println(">>> ERROR SHARE LINK: ");
            e.printStackTrace(); 
            return ResponseEntity.internalServerError().body("Gagal membuat link: " + e.getMessage());
        }
    }
}