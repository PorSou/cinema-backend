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
    private Merchant merchant = new Merchant();

    @Getter
    @Setter
    public static class Api {
        private String url = "https://api-bakong.nbc.gov.kh/v1";
    }

    @Getter
    @Setter
    public static class Merchant {
        private String token;
        private String account;
        private String name;
        private String city;
    }
}