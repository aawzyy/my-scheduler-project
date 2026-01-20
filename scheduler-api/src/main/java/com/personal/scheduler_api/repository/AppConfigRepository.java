package com.personal.scheduler_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.scheduler_api.model.AppConfig;

public interface AppConfigRepository extends JpaRepository<AppConfig, String> {
}