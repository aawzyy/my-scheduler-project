package com.personal.scheduler_api.service;

import java.time.LocalTime;

import org.springframework.stereotype.Service;

@Service
public class PreferenceScoringService {

    /**
     * Menghitung skor kualitas slot waktu berdasarkan aturan preferensi.
     * Range: 0 - 100
     */
    public int calculateScore(LocalTime start) {
        int hour = start.getHour();
        int score = 50; // Base score

        // RULE 1: Deep Work Morning (08:00 - 11:00) -> High Focus
        if (hour >= 8 && hour < 11) {
            score += 40; 
        }
        // RULE 2: After Lunch Slump (13:00 - 14:00) -> Low Energy
        else if (hour >= 13 && hour < 14) {
            score -= 20; 
        }
        // RULE 3: Late Afternoon Admin (15:00 - 17:00) -> Medium Energy
        else if (hour >= 15 && hour < 17) {
            score += 10; 
        }
        
        // RULE TAMBAHAN (Contoh masa depan):
        // - Cek apakah hari Jumat? (Kurangi skor sore)
        // - Cek apakah user baru saja ada meeting 2 jam berturut-turut?
        
        return Math.max(0, Math.min(100, score)); // Ensure 0-100
    }
}