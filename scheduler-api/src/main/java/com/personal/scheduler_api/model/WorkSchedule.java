package com.personal.scheduler_api.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
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
@Table(name = "work_schedules")
@Data // Lombok: Otomatis bikin Getter, Setter, toString
@NoArgsConstructor
@AllArgsConstructor
public class WorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Best practice: Pakai UUID, bukan Auto Increment Integer biar aman
    private UUID id;

    @Enumerated(EnumType.STRING) // Simpan di DB sebagai tulisan "MONDAY", bukan angka
    @Column(nullable = false, unique = true)
    private DayOfWeek dayOfWeek; // MONDAY, TUESDAY, dll.

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;
    
    // Field boolean ini berguna untuk menandai hari libur (off day)
    @Column(nullable = false)
    private boolean isWorkingDay;
}