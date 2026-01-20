package com.personal.scheduler_api.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "share_tokens")
@Data
@NoArgsConstructor
public class ShareToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // Ini akan jadi link uniknya (misal: a1b2-c3d4...)

    @Column(nullable = false)
    private String type; // "WORK" (Jadwal Kerja) atau "SOCIAL" (Lain-lain)

    @Column(nullable = false)
    private LocalDateTime expiresAt; // Kapan link ini mati (1 jam lagi)

    private boolean isUsed = false; // Opsional: jika link cuma boleh sekali klik

    // Constructor helper
    public ShareToken(String type, LocalDateTime expiresAt) {
        this.type = type;
        this.expiresAt = expiresAt;
    }
}