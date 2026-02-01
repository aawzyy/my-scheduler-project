package com.personal.scheduler_api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.scheduler_api.model.AppConfig;
import com.personal.scheduler_api.model.Appointment;
import com.personal.scheduler_api.model.AppointmentStatus;
import com.personal.scheduler_api.model.DecisionLog;
import com.personal.scheduler_api.model.GoogleToken;
import com.personal.scheduler_api.model.UserContact;
import com.personal.scheduler_api.repository.AppConfigRepository;
import com.personal.scheduler_api.repository.AppointmentRepository;
import com.personal.scheduler_api.repository.DecisionLogRepository;
import com.personal.scheduler_api.repository.GoogleTokenRepository;
import com.personal.scheduler_api.repository.UserContactRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository repository;
    private final DecisionLogRepository decisionLogRepository;
    private final UserContactRepository contactRepository; // Akses ke Buku Telepon
    private final AppConfigRepository configRepository; // Akses ke Settings

    // Services
    private final PreferenceScoringService scoringService;
    private final GoogleCalendarService calendarService;
    private final EmailService emailService;
    private final GoogleTokenRepository tokenRepository;

    @Transactional
    public Appointment createAppointment(Appointment appointment) {

        // --- 1. HITUNG TIME SCORE ---
        int timeScore = scoringService.calculateScore(appointment.getStartTime().toLocalTime());

        // --- 2. HITUNG RELATIONSHIP SCORE ---
        int relationshipBonus = 0;
        String contactCategory = "UNKNOWN";
        
        Optional<UserContact> contactOpt = contactRepository.findByEmail(appointment.getRequesterEmail());

        if (contactOpt.isPresent()) {
            UserContact contact = contactOpt.get();
            relationshipBonus = contact.getPriorityScore();
            contactCategory = contact.getCategory();
        }

        // --- 3. KALKULASI SKOR ---
        int finalScore = timeScore + relationshipBonus;
        int threshold = Integer.parseInt(configRepository.findById("AUTO_ACCEPT_THRESHOLD")
                        .map(AppConfig::getConfigValue).orElse("85"));

        // --- 4. KEPUTUSAN (DECISION TREE) ---
        String decisionAction = "REQUESTED";
        boolean isAutoAccepted = finalScore >= threshold;

        // [LOGIKA BLACKLIST - ANTI CRASH]
        if ("BLACKLIST".equalsIgnoreCase(contactCategory)) {
            // A. Set Status REJECTED
            appointment.setStatus(AppointmentStatus.REJECTED);
            decisionAction = "AUTO_REJECTED"; 
            finalScore = -999; 
            
            System.out.println(">>> BLACKLIST BLOCKED: " + appointment.getRequesterEmail());

            // B. Coba Kirim Email (Dengan Jaring Pengaman)
            try {
                 emailService.sendBookingStatus(
                    appointment.getRequesterEmail(), 
                    "Booking Declined", 
                    appointment.getRequesterName(), 
                    "REJECTED", 
                    appointment.getStartTime().toString()
                );
            } catch (Exception e) {
                // Kalau email gagal, CUEKIN SAJA. Jangan bikin server crash.
                System.err.println(">>> Gagal kirim email reject (Mungkin email palsu): " + e.getMessage());
            }

        } else if (isAutoAccepted) {
            // [LOGIKA AUTO ACCEPT]
            System.out.println(">>> AUTO-PILOT ACCEPTED...");
            try {
                GoogleToken token = tokenRepository.findFirstByOrderByExpiresAtDesc()
                        .orElseThrow(() -> new RuntimeException("Token Google tidak ditemukan"));
                String validToken = calendarService.getValidAccessToken(token);
                calendarService.createEvent(validToken, appointment);

                appointment.setStatus(AppointmentStatus.ACCEPTED);
                decisionAction = "AUTO_ACCEPTED";

                String emailSubject = (relationshipBonus > 0) ? "Priority Confirmed!" : "Booking Confirmed";
                
                // Email juga dibungkus try-catch biar aman
                try {
                    emailService.sendBookingStatus(
                        appointment.getRequesterEmail(), 
                        emailSubject,
                        appointment.getRequesterName(), 
                        "ACCEPTED", 
                        appointment.getStartTime().toString().replace("T", " ")
                    );
                } catch (Exception e) {
                    System.err.println(">>> Email sukses gagal dikirim: " + e.getMessage());
                }

            } catch (Exception e) {
                appointment.setStatus(AppointmentStatus.PENDING);
                decisionAction = "REQUESTED (SYNC_FAIL)";
                System.err.println(">>> Sync Google Gagal: " + e.getMessage());
            }

        } else {
            // [LOGIKA MANUAL]
            appointment.setStatus(AppointmentStatus.PENDING);
            decisionAction = "REQUESTED";
        }

        // 5. Simpan ke Database
        Appointment saved = repository.save(appointment);

        // 6. Catat Log
        try {
            String timeLabel = appointment.getStartTime().toLocalTime() + " - " + appointment.getEndTime().toLocalTime();
            String logReason = timeLabel + " | Time: " + timeScore + " + Rel: " + relationshipBonus + " (" + contactCategory + ")";
    
            DecisionLog log = new DecisionLog(
                    saved.getId(),
                    decisionAction,
                    finalScore,
                    logReason,
                    saved.getRequesterName()
            );
            decisionLogRepository.save(log);
        } catch (Exception e) {
            System.err.println(">>> Gagal simpan log (Non-fatal): " + e.getMessage());
        }

        return saved;
    }

    // Method untuk log keputusan manual (Klik tombol Terima/Tolak di Dashboard)
    public void logDecision(Appointment app, String action, String reasonDetails) {
        int currentScore = 0;
        try {
            if (app.getStartTime() != null) {
                currentScore = scoringService.calculateScore(app.getStartTime().toLocalTime());
            }
        } catch (Exception e) {
            // Ignore error score calculation for manual actions
        }

        DecisionLog log = new DecisionLog(
                app.getId(),
                action,
                currentScore,
                reasonDetails, // Alasan manual (misal: "Manual Approve")
                app.getRequesterName()
        );
        decisionLogRepository.save(log);

        System.out.println(">>> MEMORY LOGGED: " + action + " for " + app.getRequesterName());
    }

    public List<Appointment> findAll() {
        return repository.findAll();
    }

    public Appointment findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking tidak ditemukan dengan ID: " + id));
    }

    public void save(Appointment appointment) {
        repository.save(appointment);
    }
} //todo remove webook