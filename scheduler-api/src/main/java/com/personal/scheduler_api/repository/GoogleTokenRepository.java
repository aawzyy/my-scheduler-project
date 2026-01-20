package com.personal.scheduler_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.scheduler_api.model.GoogleToken;

public interface GoogleTokenRepository extends JpaRepository<GoogleToken, String> {
    // Ambil token terakhir yang disimpan
    Optional<GoogleToken> findFirstByOrderByExpiresAtDesc();
}