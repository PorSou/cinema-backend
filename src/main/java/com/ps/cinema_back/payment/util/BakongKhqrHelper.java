package com.ps.cinema_back.payment.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class BakongKhqrHelper {

    /**
     * Generates a standard KHQR string format with a valid CRC16 checksum.
     */
    public static String generateKhqrPayload(String merchantAccount, String merchantName, String city, BigDecimal amount, String currency, String txnId) {
        String currCode = "USD".equalsIgnoreCase(currency) ? "840" : "116";
        String formattedAmount = amount.stripTrailingZeros().toPlainString();

        // Standard EMVCo payload structure without the checksum trailer yet
        String payloadWithoutCrc = String.format(
                "00020101021229370016bakong@cinemaapp0109%s520459995303%s540%s5802KH5915%s6010%s62240120%s6304",
                merchantAccount, currCode, formattedAmount, merchantName, city, txnId
        );

        // Append valid CRC16 checksum
        return payloadWithoutCrc + calculateCrc16(payloadWithoutCrc);
    }

    /**
     * Calculates standard EMVCo CRC16-CCITT checksum.
     */
    private static String calculateCrc16(String data) {
        int crc = 0xFFFF;
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            crc = (crc ^ (b << 8));
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc = crc << 1;
                }
            }
        }
        crc = crc & 0xFFFF;
        return String.format("%04X", crc);
    }

    public static String calculateKhqrMd5(String qrString) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(qrString.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm unavailable", e);
        }
    }

    public static String generateQrBase64Image(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }
}