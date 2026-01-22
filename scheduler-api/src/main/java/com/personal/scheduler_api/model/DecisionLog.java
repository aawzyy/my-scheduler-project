package com.personal.scheduler_api.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table; // Jangan lupa import ini
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "decision_logs")
@Data
@NoArgsConstructor
public class DecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID appointmentId;      // Link ke appointment (bisa null jika dihapus nanti)
    private String action;           // "REQUESTED", "AUTO_ACCEPTED", "APPROVED", "REJECTED"
    private int scoreSnapshot;       // Skor saat keputusan dibuat (PENTING!)
    
    private String guestName;        // Nama tamu
    private LocalDateTime timestamp;

    // Kolom baru: Alasan / Detail perhitungan
    @Column(columnDefinition = "TEXT")
    private String reason; 

    // Constructor Helper
    public DecisionLog(UUID appointmentId, String action, int scoreSnapshot, String guestName, String reason) {
        this.appointmentId = appointmentId;
        this.action = action;
        this.scoreSnapshot = scoreSnapshot;
        this.guestName = guestName;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }
}