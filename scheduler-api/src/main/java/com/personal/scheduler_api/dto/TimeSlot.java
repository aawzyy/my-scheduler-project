package com.personal.scheduler_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // 1. Import ini

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlot {
    private String label;      
    private String startTime;  
    private int score;         
    
    // 2. Tambahkan anotasi ini di atas field boolean
    @JsonProperty("isRecommended") 
    private boolean isRecommended; 
}