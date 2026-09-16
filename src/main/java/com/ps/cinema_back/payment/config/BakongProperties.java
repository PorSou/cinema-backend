package com.ps.cinema_back.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bakong")
@Getter
@Setter
public class BakongProperties {
    private Api api = new Api();
    private Bearer bearer = new Bearer();
    private Account account = new Account();
    private Merchant merchant = new Merchant();

    @Getter
    @Setter
    public static class Api {
        private String url = "https://api-bakong.nbc.gov.kh/v1";
    }

    // 👇 maps bakong.bearer.token
    @Getter
    @Setter
    public static class Bearer {
        private String token;
    }

    // 👇 maps bakong.account.id
    @Getter
    @Setter
    public static class Account {
        private String id;
    }

    // 👇 maps bakong.merchant.name / bakong.merchant.city (new — required for the QR payload)
    @Getter
    @Setter
    public static class Merchant {
        private String name;
        private String city;
    }
}