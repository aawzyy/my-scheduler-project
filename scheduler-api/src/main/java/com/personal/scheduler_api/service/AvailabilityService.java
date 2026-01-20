package com.personal.scheduler_api.service;

import java.time.DayOfWeek; // PENTING: Import ini
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.personal.scheduler_api.dto.TimeSlot;
import com.personal.scheduler_api.model.Appointment;
import com.personal.scheduler_api.model.AppointmentStatus;
import com.personal.scheduler_api.model.GoogleToken;
import com.personal.scheduler_api.model.PersonalBlock;
import com.personal.scheduler_api.model.ShareToken;
import com.personal.scheduler_api.model.WorkSchedule;
import com.personal.scheduler_api.repository.AppointmentRepository;
import com.personal.scheduler_api.repository.GoogleTokenRepository;
import com.personal.scheduler_api.repository.PersonalBlockRepository;
import com.personal.scheduler_api.repository.ShareTokenRepository;
import com.personal.scheduler_api.repository.WorkScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final WorkScheduleRepository workScheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final ShareTokenRepository shareTokenRepository;
    private final PersonalBlockRepository personalBlockRepository;
    private final GoogleTokenRepository googleTokenRepository;
    private final GoogleCalendarService googleCalendarService;
    
    // --- INJECT DECISION ENGINE ---
    private final PreferenceScoringService scoringService;
    private final SlotRankingService rankingService;
    
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Method Utama: Mendapatkan Slot berdasarkan Token Share (Tamu)
     */
    public List<TimeSlot> getSlotsByToken(UUID tokenId, String googleAccessToken, LocalDate date) {
        
        // 1. Validasi Link Share (Token)
        ShareToken shareToken = null;
        if (tokenId != null) {
            shareToken = shareTokenRepository.findById(tokenId)
                    .orElseThrow(() -> new RuntimeException("Link tidak valid atau tidak ditemukan."));
            
            if (shareToken.isUsed()) {
                throw new RuntimeException("Link ini SUDAH DIPAKAI. Silakan minta link baru.");
            }

            if (shareToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Link sudah KADALUARSA (Expired).");
            }
        }
        
        // 2. Ambil Token Google Owner (Untuk cek busy calendar)
        GoogleToken ownerToken = googleTokenRepository.findFirstByOrderByExpiresAtDesc()
                 .orElseThrow(() -> new RuntimeException("Owner belum login ke Google Calendar."));
        String validAccessToken = googleCalendarService.getValidAccessToken(ownerToken);
        
        // 3. Fetch Data Busy (Google & Local)
        List<Map<String, String>> googleBusy = fetchBusyFromGoogle(validAccessToken, date);
        
        // Ambil data lokal (PENDING & ACCEPTED dianggap sibuk untuk mencegah double booking)
        List<Appointment> localBusy = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() != AppointmentStatus.REJECTED) // Filter yang REJECTED saja
                .filter(a -> a.getStartTime().toLocalDate().isEqual(date))
                .collect(Collectors.toList());

        // 4. Setup Range Waktu (Work vs Social)
        LocalTime startSearch;
        LocalTime endSearch;
        List<PersonalBlock> blockingBlocks = new ArrayList<>();
        
        // --- FIX DI SINI (TYPE MISMATCH) ---
        // Gunakan Enum DayOfWeek langsung, jangan di-convert ke String
        DayOfWeek dayOfWeek = date.getDayOfWeek(); 
        
        // Handle Optional return dari repository dengan .orElse(null)
        WorkSchedule workSchedule = workScheduleRepository.findByDayOfWeek(dayOfWeek).orElse(null);
        
        String tokenType = (shareToken != null) ? shareToken.getType() : "WORK"; // Default WORK

        if ("WORK".equalsIgnoreCase(tokenType)) {
            // Logic WORK: Ikuti jam kerja kaku
            if (workSchedule == null || !workSchedule.isWorkingDay()) {
                return new ArrayList<>(); // Hari libur -> Slot kosong
            }
            startSearch = workSchedule.getStartTime();
            endSearch = workSchedule.getEndTime();
        } else {
            // Logic SOCIAL: Bebas 24 jam, TAPI jam kerja dianggap 'busy' (kecuali diizinkan)
            startSearch = LocalTime.of(0, 0);
            endSearch = LocalTime.of(23, 0); // Sampai jam 11 malam
            
            // Masukkan Jam Kerja sebagai "Block" agar tidak mengganggu waktu kerja
            if (workSchedule != null && workSchedule.isWorkingDay()) {
                blockingBlocks.add(new PersonalBlock("Jam Kerja", workSchedule.getStartTime(), workSchedule.getEndTime()));
            }
            // Masukkan Personal Block lainnya (e.g. Istirahat, Gym)
            blockingBlocks.addAll(personalBlockRepository.findAll());
        }

        // 5. GENERATE RAW SLOTS (Looping per Jam)
        List<TimeSlot> rawSlots = new ArrayList<>();
        LocalTime current = startSearch;

        // Loop dari jam mulai sampai jam selesai
        while (current.plusHours(1).isBefore(endSearch) || current.plusHours(1).equals(endSearch)) {
            LocalTime next = current.plusHours(1);
            if (next.equals(LocalTime.MIN)) break; // Handle pergantian hari

            // Cek apakah slot ini bebas dari semua gangguan
            if (isSlotFree(current, next, googleBusy, localBusy, blockingBlocks)) {
                
                // --- INTEGRASI SCORING SERVICE ---
                // Hitung skor preferensi (misal: jam 10 pagi lebih disukai daripada jam 1 siang)
                int score = scoringService.calculateScore(current);
                
                String label = current.toString().substring(0, 5) + " - " + next.toString().substring(0, 5);
                String startTimeStr = current.toString().substring(0, 5);
                
                // Tambahkan ke list mentah
                // isRecommended default false, nanti diatur oleh Ranking Service
                rawSlots.add(new TimeSlot(label, startTimeStr, score, false));
            }
            
            // Pindah ke jam berikutnya
            current = next;
        }

        // 6. PANGGIL RANKING SERVICE (Sorting & Filtering)
        // Service ini akan menentukan mana slot "BEST" dan mengurutkan berdasarkan skor
        return rankingService.rankAndFilter(rawSlots);
    }

    /**
     * Helper: Cek apakah slot waktu bertabrakan dengan jadwal apapun
     */
    private boolean isSlotFree(LocalTime start, LocalTime end, 
                             List<Map<String, String>> googleBusy, 
                             List<Appointment> localBusy, 
                             List<PersonalBlock> blocks) {
        
        // A. Cek Google Calendar
        for (Map<String, String> b : googleBusy) {
            // Konversi waktu Google (OffsetDateTime) ke LocalTime Jakarta
            LocalTime bStart = OffsetDateTime.parse(b.get("start"))
                    .atZoneSameInstant(ZoneId.of("Asia/Jakarta")).toLocalTime();
            LocalTime bEnd = OffsetDateTime.parse(b.get("end"))
                    .atZoneSameInstant(ZoneId.of("Asia/Jakarta")).toLocalTime();
            
            if (isOverlapping(start, end, bStart, bEnd)) return false;
        }
        
        // B. Cek Appointment Lokal (DB)
        for (Appointment app : localBusy) {
            if (isOverlapping(start, end, app.getStartTime().toLocalTime(), app.getEndTime().toLocalTime())) {
                return false;
            }
        }
        
        // C. Cek Personal Blocks (Rutinitas / Jam Kerja di mode Social)
        for (PersonalBlock pb : blocks) {
            if (isOverlapping(start, end, pb.getStartTime(), pb.getEndTime())) {
                return false;
            }
        }
        
        return true; // Aman, tidak ada tabrakan
    }

    /**
     * Helper: Logika Matematika Overlap Waktu
     */
    private boolean isOverlapping(LocalTime slotStart, LocalTime slotEnd, LocalTime busyStart, LocalTime busyEnd) {
        // Handle jika busyEnd lewat tengah malam (jarang terjadi di appointment harian, tapi jaga-jaga)
        if (busyStart.isAfter(busyEnd)) { 
             return slotStart.isAfter(busyStart) || slotStart.equals(busyStart) || slotStart.isBefore(busyEnd);
        }
        // Logika standar: (StartA < EndB) && (EndA > StartB)
        return slotStart.isBefore(busyEnd) && slotEnd.isAfter(busyStart);
    }

    /**
     * Helper: Ambil data FreeBusy dari Google API
     */
    private List<Map<String, String>> fetchBusyFromGoogle(String accessToken, LocalDate date) {
        String url = "https://www.googleapis.com/calendar/v3/freeBusy";
        
        // Set range waktu (00:00 - 23:59 di hari tersebut)
        String timeMin = date.atStartOfDay(ZoneId.of("Asia/Jakarta")).toInstant().toString();
        String timeMax = date.atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Jakarta")).toInstant().toString();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("timeMin", timeMin);
        requestBody.put("timeMax", timeMax);
        requestBody.put("items", List.of(Map.of("id", "primary"))); // Cek kalender utama

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            // Parsing JSON Response yang kompleks dari Google
            Map calendars = (Map) response.getBody().get("calendars");
            if (calendars == null || !calendars.containsKey("primary")) return Collections.emptyList();
            
            return (List<Map<String, String>>) ((Map) calendars.get("primary")).get("busy");
            
        } catch (Exception e) {
            // Jika Google error, log errornya tapi jangan matikan aplikasi (anggap kosong/warning)
            System.err.println("Gagal sync Google Calendar: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}