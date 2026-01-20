package com.personal.scheduler_api.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Gunakan @Async agar proses kirim email tidak bikin loading frontend lama
    @Async 
    public void sendBookingStatus(String toEmail, String subject, String guestName, String status, String timeInfo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            
            String color = status.equals("ACCEPTED") ? "#10b981" : "#ef4444"; // Hijau atau Merah
            String statusText = status.equals("ACCEPTED") ? "DITERIMA" : "DITOLAK";

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 10px;">
                    <h2 style="color: #1e293b;">Halo, %s</h2>
                    <p>Status permintaan pertemuan Anda telah diperbarui.</p>
                    
                    <div style="background-color: #f8fafc; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <p style="margin: 5px 0;"><strong>Waktu:</strong> %s</p>
                        <p style="margin: 5px 0;"><strong>Status:</strong> <span style="color: %s; font-weight: bold;">%s</span></p>
                    </div>

                    <p style="color: #64748b; font-size: 12px;">Email ini dikirim otomatis oleh Personal Scheduler.</p>
                </div>
            """.formatted(guestName, timeInfo, color, statusText);

            helper.setText(htmlContent, true); // true = isHtml
            mailSender.send(message);
            
            System.out.println(">>> EMAIL TERKIRIM KE: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Gagal kirim email: " + e.getMessage());
        }
    }
}