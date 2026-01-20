package com.personal.scheduler_api.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "google_tokens")
@Data
public class GoogleToken {
    @Id
    private String email; // Kita gunakan email kamu sebagai kunci

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken; // Kolom baru untuk simpan kunci refresh

    private Instant expiresAt;
}