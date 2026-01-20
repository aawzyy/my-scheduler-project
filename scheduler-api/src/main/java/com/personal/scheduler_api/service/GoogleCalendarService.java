package com.personal.scheduler_api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value; // Import for @Value
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.personal.scheduler_api.model.Appointment;
import com.personal.scheduler_api.model.GoogleToken;
import com.personal.scheduler_api.repository.GoogleTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final GoogleTokenRepository googleTokenRepository;

    // --- SECURE INJECTION (Updated) ---
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public String getValidAccessToken(GoogleToken token) {
        // Cek apakah token masih berlaku (dengan buffer 1 menit untuk keamanan)
        if (token.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return token.getAccessToken();
        }

        System.out.println(">>> TOKEN EXPIRED, REFRESHING FOR: " + token.getEmail());

        String url = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        // USE INJECTED VALUES
        map.add("client_id", clientId); 
        map.add("client_secret", clientSecret);
        map.add("refresh_token", token.getRefreshToken());
        map.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            String newAccessToken = (String) body.get("access_token");
            Integer expiresIn = (Integer) body.get("expires_in");

            // Update data token di memori dan database
            token.setAccessToken(newAccessToken);
            token.setExpiresAt(Instant.now().plusSeconds(expiresIn));
            googleTokenRepository.save(token);

            System.out.println(">>> REFRESH SUCCESS!");
            return newAccessToken;
        } catch (Exception e) {
            System.err.println(">>> REFRESH FAILED: " + e.getMessage());
            throw new RuntimeException("Koneksi Google terputus, silakan login ulang.");
        }
    }

    public void createEvent(String accessToken, Appointment appointment) {
        String url = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        String startStr = appointment.getStartTime().atZone(ZoneId.of("Asia/Jakarta")).format(formatter);
        String endStr = appointment.getEndTime().atZone(ZoneId.of("Asia/Jakarta")).format(formatter);

        Map<String, Object> event = new HashMap<>();
        event.put("summary", appointment.getTitle());
        event.put("start", Map.of("dateTime", startStr));
        event.put("end", Map.of("dateTime", endStr));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(event, headers);
        restTemplate.postForEntity(url, entity, Map.class);
    }

    public void watchCalendar(String accessToken, String baseUrl, String userEmail) {
        String url = "https://www.googleapis.com/calendar/v3/calendars/primary/events/watch";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", UUID.randomUUID().toString());
        requestBody.put("type", "web_hook");
        requestBody.put("address", baseUrl + "/api/webhooks/google-calendar");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        try {
            restTemplate.postForEntity(url, entity, Map.class);
            System.out.println(">>> WATCH SUCCESS <<<");
        } catch (Exception e) {
            System.err.println(">>> WATCH ERROR: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getGoogleEvents(String accessToken, LocalDate start, LocalDate end) {
        String url = "https://www.googleapis.com/calendar/v3/calendars/primary/events?singleEvents=true&orderBy=startTime&timeMin={min}&timeMax={max}";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.of("UTC"));

        // Konversi waktu Jakarta ke UTC untuk query Google
        String timeMin = start.atStartOfDay(ZoneId.of("Asia/Jakarta")).format(formatter);
        String timeMax = end.atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Jakarta")).format(formatter);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class, timeMin, timeMax);

            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

            if (items == null)
                return Collections.emptyList();

            return items.stream().map(item -> {
                Map<String, Object> event = new HashMap<>();
                event.put("id", item.get("id"));
                event.put("title", item.get("summary"));
                event.put("requesterName", "Google Calendar"); 
                event.put("status", "GOOGLE"); 

                Map<String, String> startObj = (Map<String, String>) item.get("start");
                Map<String, String> endObj = (Map<String, String>) item.get("end");

                event.put("startTime", startObj.get("dateTime") != null ? startObj.get("dateTime")
                        : startObj.get("date") + "T00:00:00");
                event.put("endTime",
                        endObj.get("dateTime") != null ? endObj.get("dateTime") : endObj.get("date") + "T23:59:59");

                return event;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Gagal fetch Google Events: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}