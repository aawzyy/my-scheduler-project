package com.personal.scheduler_api.repository;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.personal.scheduler_api.model.WorkSchedule;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
    // Spring Data JPA magic: Kita cuma tulis nama method, dia bikin query sendiri
    Optional<WorkSchedule> findByDayOfWeek(DayOfWeek dayOfWeek);
}