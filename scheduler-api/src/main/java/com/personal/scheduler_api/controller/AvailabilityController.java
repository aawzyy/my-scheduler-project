package com.personal.scheduler_api.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.personal.scheduler_api.dto.TimeSlot;
import com.personal.scheduler_api.model.GoogleToken;
import com.personal.scheduler_api.repository.GoogleTokenRepository;
import com.personal.scheduler_api.service.AvailabilityService;
import com.personal.scheduler_api.service.ShareService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final GoogleTokenRepository tokenRepository;
    
    // 1. INJECT SHARE SERVICE (Untuk Validasi Token)
    private final ShareService shareService;

    @GetMapping("/token/{tokenId}")
    public ResponseEntity<?> checkAvailabilityByToken(
            @PathVariable UUID tokenId,
            @RequestParam String date) {
        
        try {
            // --- [SATPAM LOBBY] VALIDASI TOKEN DULU ---
            // Ini langkah kuncinya. Sebelum hitung jadwal, cek dulu status token.
            // Jika token sudah USED (karena submit sebelumnya) atau EXPIRED,
            // baris ini akan melempar Exception.
            shareService.validateToken(tokenId);

            // --- PROSES CALCULATION (Hanya jalan jika token Valid) ---
            
            // 1. Pastikan Owner sudah login (karena kita butuh kalender owner)
            GoogleToken ownerToken = tokenRepository.findFirstByOrderByExpiresAtDesc()
                    .orElseThrow(() -> new RuntimeException("Sistem belum siap: Owner harus login Google Calendar dulu."));

            LocalDate targetDate = LocalDate.parse(date);

            // 2. Minta Service menghitung slot waktu
            // (Method ini aman dipanggil karena token sudah divalidasi di atas)
            List<TimeSlot> slots = availabilityService.getSlotsByToken(tokenId, ownerToken.getAccessToken(), targetDate);
            
            return ResponseEntity.ok(slots);

        } catch (Exception e) {
            // TANGKAP ERROR:
            // Jika validateToken gagal, dia masuk sini.
            // Return 403 Forbidden agar Frontend tahu ini masalah izin/token mati.
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}