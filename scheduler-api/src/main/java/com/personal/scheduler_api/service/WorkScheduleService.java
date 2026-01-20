package com.personal.scheduler_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.scheduler_api.dto.WorkScheduleRequest;
import com.personal.scheduler_api.model.WorkSchedule;
import com.personal.scheduler_api.repository.WorkScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Lombok: Otomatis bikin constructor injection
public class WorkScheduleService {

    private final WorkScheduleRepository repository;

    @Transactional // Pastikan operasi database atomik
    public WorkSchedule createOrUpdateSchedule(WorkScheduleRequest request) {
        WorkSchedule schedule = repository.findByDayOfWeek(request.getDayOfWeek())
                .orElse(new WorkSchedule()); // Kalau belum ada, bikin object baru

        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setWorkingDay(request.isWorkingDay());

        return repository.save(schedule);
    }

    public List<WorkSchedule> getAllSchedules() {
        return repository.findAll();
    }
}