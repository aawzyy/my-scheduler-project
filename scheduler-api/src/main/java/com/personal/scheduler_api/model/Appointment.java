package com.personal.scheduler_api.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title; // Judul Acara (misal: Meeting Proyek)

    @Column(columnDefinition = "TEXT")
    private String description; // Detail tambahan

    @Column(nullable = false)
    private String requesterName; // Nama pengaju (misal: Klien A)
 
    @Column(nullable = false)
    private String requesterEmail; // Email pengaju

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status; // PENDING, ACCEPTED, REJECTED
}

// Kita buat enum kecil di file yang sama (atau terpisah juga boleh)
