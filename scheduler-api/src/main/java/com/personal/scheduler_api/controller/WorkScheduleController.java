package com.personal.scheduler_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.scheduler_api.dto.WorkScheduleRequest;
import com.personal.scheduler_api.model.WorkSchedule;
import com.personal.scheduler_api.service.WorkScheduleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleService service;

    @PostMapping
    public ResponseEntity<WorkSchedule> setSchedule(@RequestBody WorkScheduleRequest request) {
        return ResponseEntity.ok(service.createOrUpdateSchedule(request));
    }

    @GetMapping
    public ResponseEntity<List<WorkSchedule>> getSchedules() {
        return ResponseEntity.ok(service.getAllSchedules());
    }
}