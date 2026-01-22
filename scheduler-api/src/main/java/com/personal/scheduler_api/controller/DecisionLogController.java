package com.personal.scheduler_api.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.scheduler_api.model.DecisionLog;
import com.personal.scheduler_api.repository.DecisionLogRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class DecisionLogController {

    private final DecisionLogRepository repository;

    @GetMapping
    public ResponseEntity<List<DecisionLog>> getLogs() {
        // Ambil semua log, urutkan dari yang paling baru (DESC)
        return ResponseEntity.ok(repository.findAll(Sort.by(Sort.Direction.DESC, "timestamp")));
    }
}