package com.personal.scheduler_api.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private String action;           // "REQUESTED", "APPROVED", "REJECTED"
    private int slotScore;           // Skor slot saat keputusan dibuat (PENTING!)
    private String timeLabel;        // "09:00 - 10:00"
    private String guestName;
    private LocalDateTime timestamp;

    public DecisionLog(UUID appointmentId, String action, int slotScore, String timeLabel, String guestName) {
        this.appointmentId = appointmentId;
        this.action = action;
        this.slotScore = slotScore;
        this.timeLabel = timeLabel;
        this.guestName = guestName;
        this.timestamp = LocalDateTime.now();
    }
}