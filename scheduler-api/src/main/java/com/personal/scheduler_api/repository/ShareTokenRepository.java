package com.personal.scheduler_api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.personal.scheduler_api.model.ShareToken;

@Repository
public interface ShareTokenRepository extends JpaRepository<ShareToken, UUID> {
    // JpaRepository sudah otomatis menyediakan findById, save, delete, dll.
}