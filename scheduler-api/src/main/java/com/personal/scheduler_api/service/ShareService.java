package com.personal.scheduler_api.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.scheduler_api.model.ShareToken;
import com.personal.scheduler_api.repository.ShareTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareTokenRepository shareTokenRepository;

    // 1. Generate Link Baru
    public ShareToken createShareToken(String type) {
        // Validasi tipe (hanya boleh WORK atau SOCIAL)
        if (!type.equalsIgnoreCase("WORK") && !type.equalsIgnoreCase("SOCIAL")) {
            throw new IllegalArgumentException("Tipe harus WORK atau SOCIAL");
        }

        // Set expired 1 jam dari sekarang
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        ShareToken token = new ShareToken(type.toUpperCase(), expiresAt);
        return shareTokenRepository.save(token);
    }

    // 2. Validasi Token (Dipakai saat Tamu membuka halaman & mengecek slot)
    public ShareToken validateToken(UUID tokenId) {
        ShareToken token = shareTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Link tidak valid atau tidak ditemukan."));

        // CEK 1: Apakah sudah pernah dipakai?
        if (token.isUsed()) {
            throw new RuntimeException("Link ini SUDAH DIPAKAI (Hangus). Silakan minta link baru.");
        }

        // CEK 2: Apakah sudah expired waktunya?
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Link sudah KADALUARSA (Expired).");
        }

        return token;
    }
    
    // 3. Tandai token sudah dipakai (Dipanggil oleh Controller setelah Booking sukses)
    // FIX: Tambahkan @Transactional agar atomik
    @Transactional
    public void markAsUsed(UUID tokenId) {
        shareTokenRepository.findById(tokenId).ifPresent(token -> {
            token.setUsed(true);
            shareTokenRepository.save(token);
            System.out.println(">>> TOKEN BURNED (HANGUS): " + tokenId);
        });
    }
}