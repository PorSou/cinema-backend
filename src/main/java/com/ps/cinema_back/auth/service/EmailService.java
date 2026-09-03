package com.ps.cinema_back.auth.service;

import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.payment.util.BakongKhqrHelper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otpCode) {
        log.info("📧 [OTP DISPATCH] Destination: {} | Code: {}", toEmail, otpCode);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Cinema App - Email Verification Code");
            message.setText("Your OTP verification code is: " + otpCode + "\n\nThis code expires in 5 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("⚠️ Could not deliver OTP email to '{}': {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendETicketEmail(Booking booking) {
        String recipient = booking.getUser().getEmail();
        log.info("🎫 Sending E-Ticket email to {} for Booking #{}", recipient, booking.getBookingNumber());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject("🎬 Your Cinema E-Ticket: Booking #" + booking.getBookingNumber());

            // Generate Ticket QR Code content (contains booking number for staff scanner)
            String ticketQrRaw = "TICKET:" + booking.getBookingNumber();
            String qrBase64 = BakongKhqrHelper.generateQrBase64Image(ticketQrRaw, 250, 250);

            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #ddd; border-radius: 10px; overflow: hidden;">
                    <div style="background-color: #e50914; color: white; padding: 20px; text-align: center;">
                        <h1 style="margin: 0;">CINEMA E-TICKET</h1>
                        <p style="margin: 5px 0 0 0;">Booking #%s</p>
                    </div>
                    <div style="padding: 20px; background-color: #fcfcfc;">
                        <h2 style="color: #333; margin-top: 0;">%s</h2>
                        <table style="width: 100%%; line-height: 1.8; color: #555;">
                            <tr><td><strong>Cinema:</strong></td><td>%s</td></tr>
                            <tr><td><strong>Hall:</strong></td><td>%s</td></tr>
                            <tr><td><strong>Showtime:</strong></td><td>%s</td></tr>
                            <tr><td><strong>Seats (%d):</strong></td><td>%s</td></tr>
                            <tr><td><strong>Total Paid:</strong></td><td style="color: green; font-weight: bold;">$%s</td></tr>
                            <tr><td><strong>Customer:</strong></td><td>%s</td></tr>
                        </table>
                        <div style="text-align: center; margin-top: 25px;">
                            <p style="font-size: 13px; color: #888; margin-bottom: 8px;">Present this QR code to cinema staff at entrance</p>
                            <img src="%s" alt="Ticket QR Code" style="border: 2px solid #ccc; border-radius: 8px;"/>
                        </div>
                    </div>
                </div>
                """,
                    booking.getBookingNumber(),
                    booking.getShowtime().getMovie().getTitle(),
                    booking.getShowtime().getHall().getCinema().getName(),
                    booking.getShowtime().getHall().getName(),
                    booking.getShowtime().getStartTime().toString().replace("T", " "),
                    booking.getBookingSeats().size(),
                    booking.getBookingSeats().stream().map(s -> s.getSeat().getSeatRow() + s.getSeat().getSeatNumber()).toList(),
                    booking.getTotalAmount().toPlainString(),
                    booking.getUser().getFullName(),
                    qrBase64
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("✅ E-Ticket sent successfully to {}", recipient);
        } catch (Exception e) {
            log.warn("⚠️ Failed to deliver E-Ticket email: {}", e.getMessage());
        }
    }
}