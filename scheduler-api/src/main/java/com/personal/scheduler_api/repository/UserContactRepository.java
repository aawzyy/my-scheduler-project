package com.personal.scheduler_api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.scheduler_api.model.UserContact;

public interface UserContactRepository extends JpaRepository<UserContact, UUID> {
    Optional<UserContact> findByEmail(String email);
}