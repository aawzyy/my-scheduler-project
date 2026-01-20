package com.personal.scheduler_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.personal.scheduler_api.model.PersonalBlock;

@Repository
public interface PersonalBlockRepository extends JpaRepository<PersonalBlock, Long> {
    // Kita nanti akan ambil semua block untuk filter jadwal
}