package com.personal.scheduler_api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.scheduler_api.model.AppConfig;
import com.personal.scheduler_api.repository.AppConfigRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final AppConfigRepository repository;

    @GetMapping
    public List<AppConfig> getAllConfigs() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, String> payload) {
        String key = payload.get("key");
        String value = payload.get("value");
        
        AppConfig config = new AppConfig(key, value);
        repository.save(config);
        
        return ResponseEntity.ok(Map.of("message", "Config updated"));
    }
}