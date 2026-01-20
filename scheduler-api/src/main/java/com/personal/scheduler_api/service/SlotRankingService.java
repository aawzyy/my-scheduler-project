package com.personal.scheduler_api.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.personal.scheduler_api.dto.TimeSlot;

@Service
public class SlotRankingService {

    private static final int RECOMMENDATION_THRESHOLD = 80;

    public List<TimeSlot> rankAndFilter(List<TimeSlot> rawSlots) {
        // 1. Mark Recommended Slots
        List<TimeSlot> processed = rawSlots.stream()
            .map(slot -> {
                if (slot.getScore() >= RECOMMENDATION_THRESHOLD) {
                    slot.setRecommended(true);
                }
                return slot;
            })
            .collect(Collectors.toList());

        // 2. Sorting Strategy
        // Opsi A: Urutkan berdasarkan Waktu (Pagi -> Sore) - Default UX Kalender
        // processed.sort(Comparator.comparing(TimeSlot::getStartTime));

        // Opsi B: Urutkan berdasarkan Skor (Best -> Worst) - Default UX Assistant
        // processed.sort(Comparator.comparingInt(TimeSlot::getScore).reversed());
        
        // Saat ini kita pakai Opsi A agar user tidak bingung lihat jam lompat-lompat
        return processed;
    }
}