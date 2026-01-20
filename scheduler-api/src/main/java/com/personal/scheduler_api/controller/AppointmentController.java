package com.personal.scheduler_api.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.scheduler_api.dto.AppointmentRequest;
import com.personal.scheduler_api.model.Appointment;
import com.personal.scheduler_api.model.AppointmentStatus;
import com.personal.scheduler_api.model.GoogleToken;
import com.personal.scheduler_api.repository.GoogleTokenRepository;
import com.personal.scheduler_api.repository.PersonalBlockRepository;
import com.personal.scheduler_api.service.AppointmentService;
import com.personal.scheduler_api.service.EmailService;
import com.personal.scheduler_api.service.GoogleCalendarService;
import com.personal.scheduler_api.service.ShareService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;
    private final GoogleCalendarService calendarService;
    private final GoogleTokenRepository tokenRepository;
    private final PersonalBlockRepository personalBlockRepository;
    private final EmailService emailService;
    private final ShareService shareService;

    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(service.findAll());
    }

    // --- 1. FIX: ENDPOINT BOOKING (TAMU) ---
    @PostMapping("/appointments")
    public ResponseEntity<?> createBooking(@RequestBody AppointmentRequest request) { // Return pakai ? agar bisa kirim Error String
        
        // --- [LANGKAH 1: SATPAM] VALIDASI TOKEN DULU ---
        // Sebelum membuat appointment, pastikan token valid dan BELUM DIPAKAI.
        if (request.getShareToken() != null && !request.getShareToken().isEmpty()) {
            try {
                UUID tokenId = UUID.fromString(request.getShareToken());
                shareService.validateToken(tokenId); // Akan melempar Error jika token Expired/Used
            } catch (Exception e) {
                // JIKA GAGAL: Langsung tolak request. Jangan lanjut ke bawah!
                return ResponseEntity.status(403).body("Gagal Booking: " + e.getMessage());
            }
        }

        // --- [LANGKAH 2] PROSES BOOKING (Hanya jalan jika lolos validasi) ---
        Appointment appointment = new Appointment();
        appointment.setTitle(request.getTitle());
        appointment.setDescription(request.getDescription());
        appointment.setRequesterName(request.getRequesterName());
        appointment.setRequesterEmail(request.getRequesterEmail());
        appointment.setStartTime(request.getStartTime()); 
        appointment.setEndTime(request.getEndTime());

        Appointment saved = service.createAppointment(appointment);

        // --- [LANGKAH 3] BAKAR TOKEN (Setelah sukses) ---
        if (request.getShareToken() != null && !request.getShareToken().isEmpty()) {
            try {
                UUID tokenId = UUID.fromString(request.getShareToken());
                shareService.markAsUsed(tokenId);
                System.out.println(">>> TOKEN BURNED: " + tokenId);
            } catch (Exception e) {
                // Token gagal dibakar tapi booking sudah jadi (log warning saja)
                System.err.println("Warning: Gagal mematikan token: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(saved);
    }

    // --- 2. FIX: APPROVE (OWNER) ---
    @PostMapping("/appointments/{id}/approve")
    public ResponseEntity<?> approveAppointment(@PathVariable UUID id) {
        Appointment appointment = service.findById(id);
        
        try {
            // A. Ambil Token Valid untuk Sync ke Google
            GoogleToken token = tokenRepository.findFirstByOrderByExpiresAtDesc()
                    .orElseThrow(() -> new RuntimeException("Token Google tidak ditemukan. Owner harus login."));
            String validToken = calendarService.getValidAccessToken(token);

            // B. Push ke Google Calendar
            calendarService.createEvent(validToken, appointment);
            
            // C. Update Status DB Lokal
            appointment.setStatus(AppointmentStatus.ACCEPTED);
            service.save(appointment);
            
            // D. MEMORY LAYER: Catat Keputusan
            service.logDecision(appointment, "APPROVED");

            // E. Kirim Email Notifikasi
            String timeStr = appointment.getStartTime().toString().replace("T", " ");
            emailService.sendBookingStatus(
                appointment.getRequesterEmail(), 
                "Pertemuan Dikonfirmasi: " + appointment.getTitle(),
                appointment.getRequesterName(),
                "ACCEPTED",
                timeStr
            );

            return ResponseEntity.ok(Map.of("message", "Approved & Synced"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Gagal menyetujui: " + e.getMessage());
        }
    }

    // --- 3. FIX: REJECT (OWNER) ---
    @PostMapping("/appointments/{id}/reject")
    public ResponseEntity<?> rejectAppointment(@PathVariable UUID id) {
        Appointment appointment = service.findById(id);
        
        // A. Update Status DB Lokal
        appointment.setStatus(AppointmentStatus.REJECTED);
        service.save(appointment);

        // B. MEMORY LAYER: Catat Keputusan
        service.logDecision(appointment, "REJECTED");

        // C. Kirim Email Notifikasi
        String timeStr = appointment.getStartTime().toString().replace("T", " ");
        emailService.sendBookingStatus(
            appointment.getRequesterEmail(), 
            "Pertemuan Ditolak: " + appointment.getTitle(),
            appointment.getRequesterName(),
            "REJECTED",
            timeStr
        );

        return ResponseEntity.ok(Map.of("message", "Booking ditolak."));
    }

    // --- 4. FIX: QUICK TASK (OWNER) ---
    @PostMapping("/appointments/tasks/quick")
    public ResponseEntity<?> createQuickTask(@RequestBody AppointmentRequest request) {
        System.out.println(">>> MEMPROSES QUICK TASK...");

        try {
            GoogleToken token = tokenRepository.findFirstByOrderByExpiresAtDesc()
                    .orElseThrow(() -> new RuntimeException("Silakan login Google dulu"));
            String validToken = calendarService.getValidAccessToken(token);

            // A. Mapping Manual DTO -> Entity
            Appointment task = new Appointment();
            task.setTitle(request.getTitle());
            task.setDescription("Quick Task");
            task.setRequesterName("Owner");
            task.setRequesterEmail(request.getRequesterEmail()); // Email owner
            
            // FIX: Langsung assign tanpa parse
            task.setStartTime(request.getStartTime());
            task.setEndTime(request.getEndTime());

            // B. Simpan ke Database (Akan masuk log REQUESTED via service)
            Appointment savedTask = service.createAppointment(task);
            
            // C. Langsung Accept
            savedTask.setStatus(AppointmentStatus.ACCEPTED);
            service.save(savedTask);

            // D. Sync ke Google Calendar
            calendarService.createEvent(validToken, savedTask);
            
            // E. Log Decision (Auto Approved)
            service.logDecision(savedTask, "AUTO-CREATED");

            return ResponseEntity.ok(Map.of("message", "Quick Task Berhasil Disinkronkan!"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Gagal membuat task: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard/status")
    public ResponseEntity<?> getDashboardStatus() {
        Optional<GoogleToken> token = tokenRepository.findFirstByOrderByExpiresAtDesc();
        Map<String, Object> status = new HashMap<>();
        if (token.isPresent()) {
            status.put("email", token.get().getEmail());
            status.put("isExpired", token.get().getExpiresAt().isBefore(Instant.now()));
        } else {
            status.put("isExpired", true);
        }
        status.put("personalBlocks", personalBlockRepository.findAll());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/dashboard/combined")
    public ResponseEntity<List<Map<String, Object>>> getDashboardEvents() {
        try {
            // 1. Ambil Token & Refresh jika perlu
            GoogleToken token = tokenRepository.findFirstByOrderByExpiresAtDesc()
                    .orElseThrow(() -> new RuntimeException("Owner belum login"));
            String validToken = calendarService.getValidAccessToken(token);

            // 2. Ambil Appointment Lokal
            List<Appointment> localApps = service.findAll();

            // 3. Ambil Event Google
            LocalDate start = LocalDate.now().minusMonths(1);
            LocalDate end = LocalDate.now().plusMonths(3);
            List<Map<String, Object>> googleEvents = calendarService.getGoogleEvents(validToken, start, end);

            // 4. GABUNGKAN DATA DENGAN FILTER DUPLIKAT
            List<Map<String, Object>> combined = new ArrayList<>();

            // A. Masukkan SEMUA Data Lokal dulu
            for (Appointment app : localApps) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", app.getId());
                m.put("title", app.getTitle());
                m.put("requesterName", app.getRequesterName());
                m.put("requesterEmail", app.getRequesterEmail());
                m.put("startTime", app.getStartTime().toString());
                m.put("endTime", app.getEndTime().toString());
                m.put("status", app.getStatus().toString());
                combined.add(m);
            }

            // B. Masukkan Data Google HANYA JIKA TIDAK ADA di Data Lokal (Cek Duplikat)
            for (Map<String, Object> gEvent : googleEvents) {
                String gTitle = (String) gEvent.get("title");
                String gStartRaw = (String) gEvent.get("startTime");
                // Ambil 16 karakter pertama (YYYY-MM-DDTHH:mm) untuk perbandingan
                String gStart = gStartRaw != null && gStartRaw.length() > 16 ? gStartRaw.substring(0, 16) : gStartRaw;

                boolean isDuplicate = localApps.stream().anyMatch(local -> {
                    // Cek hanya yang statusnya ACCEPTED
                    if (local.getStatus() != AppointmentStatus.ACCEPTED) return false;

                    String localStart = local.getStartTime().toString();
                    
                    // Bandingkan Judul & Waktu Mulai (Ignore Case)
                    return gTitle != null && local.getTitle().equalsIgnoreCase(gTitle) && 
                           localStart.startsWith(gStart);
                });

                if (!isDuplicate) {
                    combined.add(gEvent);
                }
            }

            return ResponseEntity.ok(combined);
            
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}