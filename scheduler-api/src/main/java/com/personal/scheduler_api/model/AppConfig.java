package com.personal.scheduler_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_configs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppConfig {
    @Id
    private String configKey; // Contoh: "AUTO_ACCEPT_THRESHOLD"
    private String configValue; // Contoh: "85"
}