package com.personal.scheduler_api.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AppointmentRequest {
    private String title;
    private String description;
    private String requesterName;
    private String requesterEmail;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String shareToken;
}