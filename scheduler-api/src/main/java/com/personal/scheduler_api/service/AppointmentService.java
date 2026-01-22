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

        // --- 1. HITUNG TIME SCORE (Faktor Waktu) ---
        // Contoh: Jam 10 pagi = 50 poin. Jam 1 siang = 30 poin.
        int timeScore = scoringService.calculateScore(appointment.getStartTime().toLocalTime());

        // --- 2. HITUNG RELATIONSHIP SCORE (Faktor Siapa) ---
        int relationshipBonus = 0;
        String contactCategory = "UNKNOWN"; // Default orang asing
        
        // Cek apakah email tamu ada di database kontak?
        Optional<UserContact> contactOpt = contactRepository.findByEmail(appointment.getRequesterEmail());

        if (contactOpt.isPresent()) {
            UserContact contact = contactOpt.get();
            relationshipBonus = contact.getPriorityScore();
            contactCategory = contact.getCategory();
            
            System.out.println(">>> RELATIONSHIP DETECTED: " + contact.getName() + " (" + contactCategory + ") Bonus: " + relationshipBonus);
        }

        // --- 3. KALKULASI SKOR AKHIR ---
        int finalScore = timeScore + relationshipBonus;

        // Ambil Threshold Global dari Database (Default 85)
        int threshold = Integer.parseInt(
                configRepository.findById("AUTO_ACCEPT_THRESHOLD")
                        .map(AppConfig::getConfigValue)
                        .orElse("85"));

        // --- 4. LOGIKA PENGAMBILAN KEPUTUSAN (DECISION TREE) ---
        String decisionAction = "REQUESTED";
        boolean isAutoAccepted = finalScore >= threshold;

        // [PERISAI BESI] Cek Blacklist DULUAN
        if ("BLACKLIST".equalsIgnoreCase(contactCategory)) {
            // A. Langsung Tolak (Tanpa Pending)
            appointment.setStatus(AppointmentStatus.REJECTED);
            decisionAction = "AUTO_REJECTED"; 
            finalScore = -999; // Set skor hancur

            System.out.println(">>> BLACKLIST KICKED OUT: " + appointment.getRequesterEmail());

            // B. Kirim Email Penolakan (Opsional, biar sopan dikit)
             emailService.sendBookingStatus(
                appointment.getRequesterEmail(), 
                "Booking Status Update", 
                appointment.getRequesterName(), 
                "REJECTED", 
                appointment.getStartTime().toString()
            );

        } else if (isAutoAccepted) {
            // [JALUR VIP] Auto Accept jika Skor Cukup
            System.out.println(">>> AUTO-PILOT ACCEPTED: Final Score " + finalScore + " (Threshold " + threshold + ")");

            try {
                // A. Sync ke Google Calendar
                GoogleToken token = tokenRepository.findFirstByOrderByExpiresAtDesc()
                        .orElseThrow(() -> new RuntimeException("Token Google tidak ditemukan"));
                String validToken = calendarService.getValidAccessToken(token);
                calendarService.createEvent(validToken, appointment);

                // B. Update Status jadi ACCEPTED
                appointment.setStatus(AppointmentStatus.ACCEPTED);
                decisionAction = "AUTO_ACCEPTED";

                // C. Email Notifikasi Sukses
                String timeStr = appointment.getStartTime().toString().replace("T", " ");
                String emailSubject = "Booking Confirmed: " + appointment.getTitle();

                // Jika VIP, kasih subjek lebih spesial
                if (relationshipBonus > 0) {
                    emailSubject = "Priority Confirmation: " + appointment.getTitle();
                }

                emailService.sendBookingStatus(
                        appointment.getRequesterEmail(),
                        emailSubject,
                        appointment.getRequesterName(),
                        "ACCEPTED",
                        timeStr);

            } catch (Exception e) {
                System.err.println(">>> GAGAL AUTO-SYNC: " + e.getMessage());
                // Fallback: Jika Google error, jangan tolak, tapi jadikan PENDING
                appointment.setStatus(AppointmentStatus.PENDING);
                decisionAction = "REQUESTED (SYNC_FAIL)";
            }

        } else {
            // [RUANG TUNGGU] Skor tidak cukup -> Manual Review
            appointment.setStatus(AppointmentStatus.PENDING);
            decisionAction = "REQUESTED";
        }

        // 5. Simpan Status Akhir ke Database
        Appointment saved = repository.save(appointment);

        // 6. Catat ke Memori (Log)
        String timeLabel = appointment.getStartTime().toLocalTime() + " - " + appointment.getEndTime().toLocalTime();
        
        // Format Log: "10:00 - 11:00 | Time: 50 + Rel: 50 (VIP)"
        String logReason = timeLabel + " | Time: " + timeScore + " + Rel: " + relationshipBonus + " (" + contactCategory + ")";

        DecisionLog log = new DecisionLog(
                saved.getId(),
                decisionAction,
                finalScore,
                logReason, // Alasan detail
                saved.getRequesterName() // Nama Tamu
        );
        decisionLogRepository.save(log);

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
}