package com.personal.scheduler_api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.scheduler_api.model.DecisionLog;

public interface DecisionLogRepository extends JpaRepository<DecisionLog, UUID> {
    // Nanti kita bisa tambah query statistik di sini
    // Contoh: Mencari rata-rata skor appointment yang di-REJECT
}