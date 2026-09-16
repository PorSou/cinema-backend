package com.ps.cinema_back.payment.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class BakongKhqrHelper {

    // KHQR dynamic QR codes must carry an expiry; Bakong's own spec caps
    // this at 10 minutes ("The QR code time-out shall not exceed 10 mins").
    private static final long QR_EXPIRY_MILLIS = 10 * 60 * 1000L;

    /**
     * Generates a KHQR payload using the official NBC SDK instead of a
     * hand-rolled EMVCo/TLV encoder. The SDK owns CRC16, tag lengths, and
     * field ordering, so this is correct-by-construction rather than
     * something we have to get right by hand.
     */
    public static String generateKhqrPayload(
            String merchantAccount,
            String merchantName,
            String city,
            BigDecimal amount,
            String currency,
            String txnId
    ) {
        IndividualInfo individualInfo = new IndividualInfo();
        individualInfo.setBakongAccountId(merchantAccount);
        individualInfo.setMerchantName(merchantName);
        individualInfo.setMerchantCity(city);
        individualInfo.setCurrency(
                "USD".equalsIgnoreCase(currency) ? KHQRCurrency.USD : KHQRCurrency.KHR
        );
        individualInfo.setAmount(amount.doubleValue());
        individualInfo.setBillNumber(txnId);
        // Mandatory for dynamic (amount-bearing) KHQR per NBC spec —
        // omitting this triggers Bakong error code 45 / a scanner-side
        // "invalid format" rejection.
        individualInfo.setExpirationTimestamp(System.currentTimeMillis() + QR_EXPIRY_MILLIS);

        KHQRResponse<KHQRData> response = BakongKHQR.generateIndividual(individualInfo);

        if (response.getKHQRStatus().getCode() != 0) {
            throw new IllegalStateException(
                    "KHQR generation failed: " + response.getKHQRStatus().getMessage()
                            + " (errorCode=" + response.getKHQRStatus().getErrorCode() + ")"
            );
        }

        return response.getData().getQr();
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