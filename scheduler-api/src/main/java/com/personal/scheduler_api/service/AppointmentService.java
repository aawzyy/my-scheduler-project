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
    private final UserContactRepository contactRepository; // NEW: Akses ke Buku Telepon
    private final AppConfigRepository configRepository;    // Akses ke Settings

    // Services
    private final PreferenceScoringService scoringService;
    private final GoogleCalendarService calendarService;
    private final EmailService emailService;
    private final GoogleTokenRepository tokenRepository;

    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        
        // --- 1. HITUNG TIME SCORE (Faktor Waktu) ---
        int timeScore = scoringService.calculateScore(appointment.getStartTime().toLocalTime());
        
        // --- 2. HITUNG RELATIONSHIP SCORE (Faktor Siapa) ---
        int relationshipBonus = 0;
        String contactCategory = "UNKNOWN";
        String contactName = "Guest";

        // Cek apakah email tamu ada di database kontak?
        Optional<UserContact> contactOpt = contactRepository.findByEmail(appointment.getRequesterEmail());
        
        if (contactOpt.isPresent()) {
            UserContact contact = contactOpt.get();
            relationshipBonus = contact.getPriorityScore();
            contactCategory = contact.getCategory();
            contactName = contact.getName();
            System.out.println(">>> RELATIONSHIP DETECTED: " + contactName + " (" + contactCategory + ") Bonus: " + relationshipBonus);
        }

        // --- 3. KALKULASI SKOR AKHIR ---
        int finalScore = timeScore + relationshipBonus;

        // Ambil Threshold Global dari Database (Default 85)
        int threshold = Integer.parseInt(
            configRepository.findById("AUTO_ACCEPT_THRESHOLD")
                .map(AppConfig::getConfigValue)
                .orElse("85")
        );

        // --- 4. LOGIKA PENGAMBILAN KEPUTUSAN ---
        boolean isAutoAccepted = finalScore >= threshold;
        String decisionAction = "REQUESTED";

        // Safety Guard: Jika Blacklist, paksa tolak otomatis (atau set skor minus)
        if ("BLACKLIST".equalsIgnoreCase(contactCategory)) {
            isAutoAccepted = false;
            finalScore = -999; 
            System.out.println(">>> BLACKLIST BLOCKED: " + appointment.getRequesterEmail());
        }

        if (isAutoAccepted) {
            System.out.println(">>> AUTO-PILOT ACCEPTED: Final Score " + finalScore + " (Threshold " + threshold + ")");
            
            try {
                // A. Sync Google
                GoogleToken token = tokenRepository.findFirstByOrderByExpiresAtDesc()
                    .orElseThrow(() -> new RuntimeException("Token Google tidak ditemukan"));
                String validToken = calendarService.getValidAccessToken(token);
                calendarService.createEvent(validToken, appointment);
                
                // B. Update Status
                appointment.setStatus(AppointmentStatus.ACCEPTED);
                decisionAction = "AUTO_ACCEPTED";

                // C. Email Notifikasi
                String timeStr = appointment.getStartTime().toString().replace("T", " ");
                String emailSubject = "⚡ Otomatis Diterima: " + appointment.getTitle();
                
                // Jika VIP, kasih subjek lebih sopan
                if (relationshipBonus > 0) {
                    emailSubject = "Priority Confirmed: " + appointment.getTitle();
                }

                emailService.sendBookingStatus(
                    appointment.getRequesterEmail(), 
                    emailSubject,
                    appointment.getRequesterName(), 
                    "ACCEPTED", 
                    timeStr
                );

            } catch (Exception e) {
                System.err.println(">>> GAGAL AUTO-SYNC: " + e.getMessage());
                // Fallback ke manual jika error
                appointment.setStatus(AppointmentStatus.PENDING);
                isAutoAccepted = false;
                decisionAction = "REQUESTED (SYNC_FAIL)";
            }

        } else {
            // Skor tidak cukup -> Manual Review
            appointment.setStatus(AppointmentStatus.PENDING);
            decisionAction = "REQUESTED";
        }

        // 5. Simpan ke Database
        Appointment saved = repository.save(appointment);

        // 6. Catat Log Memori (Penting untuk audit)
        String timeLabel = appointment.getStartTime().toLocalTime() + " - " + appointment.getEndTime().toLocalTime();
        
        // Kita simpan detail matematikanya di kolom 'guestName' atau field baru jika ada
        // Format Log: "09:00 - 10:00 | Time: 30 + Rel: 60 (VIP)"
        String logDetail = timeLabel + " | Time: " + timeScore + " + Rel: " + relationshipBonus + " (" + contactCategory + ")";

        DecisionLog log = new DecisionLog(
            saved.getId(), 
            decisionAction, 
            finalScore, 
            logDetail, // Simpan detail perhitungan di sini agar terlihat di DB
            saved.getRequesterName()
        );
        decisionLogRepository.save(log);

        return saved;
    }
    
    // Method untuk log keputusan manual dari Dashboard Owner
    public void logDecision(Appointment app, String action) {
        // Hitung ulang skor saat ini sebagai referensi
        int timeScore = scoringService.calculateScore(app.getStartTime().toLocalTime());
        String timeLabel = app.getStartTime().toLocalTime() + " - " + app.getEndTime().toLocalTime();

        DecisionLog log = new DecisionLog(
            app.getId(), 
            action, 
            timeScore, // Manual decision biasanya hanya melihat time score context
            timeLabel + " (Manual Decision)", 
            app.getRequesterName()
        );
        decisionLogRepository.save(log);
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