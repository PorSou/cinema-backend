package com.ps.cinema_back.telegrambot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    // Unchanged signature — kept so any other call site (if one exists)
    // keeps compiling and behaving exactly as before, with no discount line.
    public void sendBookingSuccessNotification(
            String bookingNumber,
            String movieTitle,
            String cinemaName,
            String hallName,
            List<String> seatDetails,
            List<String> concessionDetails,
            Double totalAmount
    ) {
        sendBookingSuccessNotification(
                bookingNumber, movieTitle, cinemaName, hallName,
                seatDetails, concessionDetails, totalAmount,
                null, null
        );
    }

    // Overload — adds an optional "Voucher Discount" line, same idea
    // as the checkout confirmation summary (Subtotal / Voucher Discount /
    // Total). Only appears when discountAmount is actually > 0, so a
    // booking with no voucher looks exactly like it did before.
    public void sendBookingSuccessNotification(
            String bookingNumber,
            String movieTitle,
            String cinemaName,
            String hallName,
            List<String> seatDetails,
            List<String> concessionDetails,
            Double totalAmount,
            Double discountAmount,
            String voucherCode
    ) {

        if (!isConfigured()) {
            log.warn(
                    "Telegram bot not configured (missing token/chat-id) — skipping CASH booking notification for booking {}",
                    bookingNumber
            );
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        // Safe fallbacks in case any database field is null
        String ref = bookingNumber != null ? bookingNumber : "N/A";
        String movie = movieTitle != null ? movieTitle : "Unknown Movie";
        String cinema = cinemaName != null ? cinemaName : "CinemaX Branch";
        String hall = hallName != null ? hallName : "Standard Hall";
        double amount = totalAmount != null ? totalAmount : 0.0;
        double discount = discountAmount != null ? discountAmount : 0.0;

        StringBuilder message = new StringBuilder();
        message.append("🎬 *New Cash Booking Successful!*\n\n");
        message.append("🎟 *Ref:* ").append(ref).append("\n");
        message.append("🎥 *Movie:* ").append(movie).append("\n");
        message.append("🏢 *Cinema:* ").append(cinema).append("\n");
        message.append("📺 *Hall:* ").append(hall).append("\n");

        // Seats block
        if (seatDetails != null && !seatDetails.isEmpty()) {
            message.append("💺 *Seats (").append(seatDetails.size()).append("):* ")
                    .append(String.join(", ", seatDetails)).append("\n");
        } else {
            message.append("💺 *Seats:* N/A\n");
        }

        // F&B block — only appears when snacks were actually ordered
        if (concessionDetails != null && !concessionDetails.isEmpty()) {
            message.append("🍿 *F&B (").append(concessionDetails.size()).append("):* ")
                    .append(String.join(", ", concessionDetails)).append("\n");
        }

        // Voucher/discount block — only appears when a voucher was
        // actually applied and knocked something off the price. Shown as
        // Subtotal -> Voucher Discount -> Total, mirroring the checkout
        // confirmation summary the customer already saw.
        if (discount > 0) {
            double subtotal = amount + discount;
            message.append("🧾 *Subtotal:* $").append(String.format("%.2f", subtotal)).append("\n");
            message.append("🎫 *Voucher").append(voucherCode != null ? " (" + voucherCode + ")" : "")
                    .append(":* -$").append(String.format("%.2f", discount)).append("\n");
        }

        message.append("💵 *Total Paid:* $").append(String.format("%.2f", amount)).append(" (Cash)");

        sendTelegramMessage(url, message.toString(), "CASH", ref);
    }

    // KHQR/Bakong equivalent of sendBookingSuccessNotification.
    // Kept as its own method (not an overload) so the existing Cash
    // message wording/behavior is never touched by this change.
    public void sendKhqrPaymentSuccessNotification(
            String bookingNumber,
            String movieTitle,
            String cinemaName,
            String hallName,
            List<String> seatDetails,
            List<String> concessionDetails,
            Double totalAmount,
            Double discountAmount,
            String voucherCode
    ) {

        if (!isConfigured()) {
            log.warn(
                    "Telegram bot not configured (missing token/chat-id) — skipping KHQR payment notification for booking {}",
                    bookingNumber
            );
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        String ref = bookingNumber != null ? bookingNumber : "N/A";
        String movie = movieTitle != null ? movieTitle : "Unknown Movie";
        String cinema = cinemaName != null ? cinemaName : "CinemaX Branch";
        String hall = hallName != null ? hallName : "Standard Hall";
        double amount = totalAmount != null ? totalAmount : 0.0;
        double discount = discountAmount != null ? discountAmount : 0.0;

        StringBuilder message = new StringBuilder();
        message.append("📱 *New KHQR Payment Successful!*\n\n");
        message.append("🎟 *Ref:* ").append(ref).append("\n");
        message.append("🎥 *Movie:* ").append(movie).append("\n");
        message.append("🏢 *Cinema:* ").append(cinema).append("\n");
        message.append("📺 *Hall:* ").append(hall).append("\n");

        if (seatDetails != null && !seatDetails.isEmpty()) {
            message.append("💺 *Seats (").append(seatDetails.size()).append("):* ")
                    .append(String.join(", ", seatDetails)).append("\n");
        } else {
            message.append("💺 *Seats:* N/A\n");
        }

        if (concessionDetails != null && !concessionDetails.isEmpty()) {
            message.append("🍿 *F&B (").append(concessionDetails.size()).append("):* ")
                    .append(String.join(", ", concessionDetails)).append("\n");
        }

        if (discount > 0) {
            double subtotal = amount + discount;
            message.append("🧾 *Subtotal:* $").append(String.format("%.2f", subtotal)).append("\n");
            message.append("🎫 *Voucher").append(voucherCode != null ? " (" + voucherCode + ")" : "")
                    .append(":* -$").append(String.format("%.2f", discount)).append("\n");
        }

        message.append("💵 *Total Paid:* $").append(String.format("%.2f", amount)).append(" (Bakong KHQR)");

        sendTelegramMessage(url, message.toString(), "KHQR", ref);
    }

    // =========================================================
    // SHARED SEND + CONFIG HELPERS
    // =========================================================

    private boolean isConfigured() {
        return botToken != null && !botToken.isBlank()
                && chatId != null && !chatId.isBlank();
    }

    private void sendTelegramMessage(String url, String text, String context, String ref) {
        Map<String, String> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        payload.put("parse_mode", "Markdown");

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url, requestEntity, String.class);

            log.info("Telegram {} notification sent successfully for ref={}", context, ref);
        } catch (Exception e) {
            // This used to be e.printStackTrace(), which is easy to miss in
            // production logs. Logging it properly here so a bad bot
            // token, network block, or Telegram API error is visible.
            log.error(
                    "Failed to send Telegram {} notification for ref={}: {}",
                    context,
                    ref,
                    e.getMessage(),
                    e
            );
        }
    }
}