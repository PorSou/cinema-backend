package com.ps.cinema_back.payment.client;

import com.ps.cinema_back.payment.config.BakongProperties;
import com.ps.cinema_back.payment.dto.response.BakongCheckMd5Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BakongClient {

    private final RestTemplate restTemplate;
    private final BakongProperties bakongProperties;

    public BakongCheckMd5Response checkTransactionByMd5(String md5Hash) {
        String url = bakongProperties.getApi().getUrl() + "/check_transaction_by_md5";
        String token = bakongProperties.getMerchant().getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            headers.set("Authorization", "Bearer " + token);
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("md5", md5Hash);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<BakongCheckMd5Response> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    BakongCheckMd5Response.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to check transaction by MD5 from Bakong: {}", e.getMessage());
            return null;
        }
    }
}