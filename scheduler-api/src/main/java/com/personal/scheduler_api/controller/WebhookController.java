package com.personal.scheduler_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    @PostMapping("/google-calendar")
    public ResponseEntity<Void> handleGoogleNotify(
            @RequestHeader(value = "X-Goog-Resource-ID", required = false) String resourceId,
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String state,
            @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId) {
        
        System.out.println("\n==========================================");
        System.out.println(">>> GOOGLE CALENDAR WEBHOOK DETECTED <<<");
        System.out.println("Channel ID : " + channelId);
        System.out.println("Resource ID: " + resourceId);
        System.out.println("State      : " + state); 
        
        if ("exists".equalsIgnoreCase(state)) {
            System.out.println(">>> UPDATE: Perubahan terdeteksi di Google Calendar user.");
            System.out.println(">>> Link Share Tamu akan otomatis menampilkan jadwal terbaru.");
        } else if ("sync".equalsIgnoreCase(state)) {
            System.out.println(">>> INFO: Webhook channel berhasil dibuat (Sync Token).");
        }
        System.out.println("==========================================\n");
        
        return ResponseEntity.ok().build();
    }
}