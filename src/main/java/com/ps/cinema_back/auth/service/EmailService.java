package com.ps.cinema_back.auth.service;

import com.ps.cinema_back.booking.entity.Booking;
import com.ps.cinema_back.common.exception.BadRequestException;
import com.ps.cinema_back.payment.util.BakongKhqrHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EmailService {

    private static final String MAILJET_URL = "https://api.mailjet.com/v3.1/send";
    private static final Pattern ERROR_STATUS = Pattern.compile("\"Status\"\\s*:\\s*\"error\"");

    // Empty defaults so the app still starts if the keys are not configured yet.
    @Value("${mail.api-key:}") private String apiKey;
    @Value("${mail.api-secret:}") private String apiSecret;
    @Value("${mail.sender-email:}") private String senderEmail;
    @Value("${mail.sender-name:CinemaX}") private String senderName;

    private final RestTemplate rest = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    /** Synchronous on purpose: if sending fails, the caller (register / resend) must know. */
    public void sendOtpEmail(String toEmail, String otpCode) {
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto\">"
                + "<h2>CinemaX verification code</h2>"
                + "<p style=\"font-size:32px;letter-spacing:6px;font-weight:bold\">" + otpCode + "</p>"
                + "<p>This code expires in 5 minutes. If you didn't request it, ignore this email.</p>"
                + "</div>";
        try {
            send(toEmail, "CinemaX - Email Verification Code", html, null);
            log.info("OTP email sent to {}", toEmail); // never log the code itself
        } catch (Exception e) {
            log.error("Could not deliver OTP email to '{}': {}", toEmail, e.getMessage());
            throw new BadRequestException("Could not send the verification email. Please try again in a moment.");
        }
    }

    @Async
    public void sendETicketEmail(Booking booking) {
        String recipient = booking.getUser().getEmail();
        log.info("Sending E-Ticket email to {} for Booking #{}", recipient, booking.getBookingNumber());

        try {
            String qr = BakongKhqrHelper.generateQrBase64Image("TICKET:" + booking.getBookingNumber(), 250, 250);
            // The API wants raw base64, so strip a "data:image/png;base64," prefix if the helper adds one
            String qrData = qr.contains(",") ? qr.substring(qr.indexOf(',') + 1) : qr;

            String html = String.format("""
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
                        <p style="text-align:center; margin-top:25px; font-size:13px; color:#888;">
                            Your entry QR code is attached (ticket-qr.png). Show it to cinema staff at the entrance.
                        </p>
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
                    booking.getUser().getFullName()
            );

            send(recipient, "Your Cinema E-Ticket: Booking #" + booking.getBookingNumber(), html,
                    List.of(Map.of("name", "ticket-qr.png", "content", qrData)));
            log.info("E-Ticket sent successfully to {}", recipient);
        } catch (Exception e) {
            log.warn("Failed to deliver E-Ticket email: {}", e.getMessage());
        }
    }

    private void send(String to, String subject, String html, List<Map<String, String>> attachments) {
        if (apiKey.isBlank() || apiSecret.isBlank() || senderEmail.isBlank()) {
            throw new IllegalStateException(
                    "Email provider is not configured (set MAIL_API_KEY, MAIL_API_SECRET, MAIL_SENDER_EMAIL)");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(apiKey, apiSecret);

        Map<String, Object> message = new HashMap<>();
        message.put("From", Map.of("Email", senderEmail, "Name", senderName));
        message.put("To", List.of(Map.of("Email", to)));
        message.put("Subject", subject);
        message.put("HTMLPart", html);
        if (attachments != null && !attachments.isEmpty()) {
            message.put("Attachments", attachments.stream().map(a -> Map.of(
                    "ContentType", "image/png",
                    "Filename", a.get("name"),
                    "Base64Content", a.get("content"))).toList());
        }

        try {
            ResponseEntity<String> response = rest.postForEntity(
                    MAILJET_URL,
                    new HttpEntity<>(Map.of("Messages", List.of(message)), headers),
                    String.class);

            // Mailjet can answer 200 while an individual message failed, so check the body too.
            String body = response.getBody();
            if (body != null && ERROR_STATUS.matcher(body).find()) {
                throw new IllegalStateException("Mailjet rejected the message: " + body);
            }
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("Mailjet " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
    }
}