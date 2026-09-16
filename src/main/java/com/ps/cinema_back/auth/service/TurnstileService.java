package com.ps.cinema_back.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TurnstileService {

    @Value("${turnstile.verify-url}")
    private String verifyUrl;

    @Value("${turnstile.secret-key}")
    private String secretKey;

    public boolean verifyToken(String token) {

        if (token == null || token.isEmpty()) {
            return false;
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody =
                new LinkedMultiValueMap<>();

        requestBody.add("secret", secretKey);
        requestBody.add("response", token);

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            verifyUrl,
                            entity,
                            Map.class
                    );

            if (response.getStatusCode() == HttpStatus.OK
                    && response.getBody() != null) {

                Boolean success =
                        (Boolean) response.getBody().get("success");

                return Boolean.TRUE.equals(success);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}