package com.ps.cinema_back.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BakongCheckMd5Response {

    private Integer responseCode; // 0 = Success / Paid
    private String responseMessage;
    private BakongTransactionData data;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BakongTransactionData {
        private String hash;
        private String fromAccountId;
        private String toAccountId;
        private BigDecimal amount;
        private String currency; // "USD" or "KHR"
        private String description;
        private Long createdDateMs;
    }

    public boolean isSuccessful() {
        return responseCode != null && responseCode == 0 && data != null;
    }
}