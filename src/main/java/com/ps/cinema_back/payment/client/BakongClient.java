package com.ps.cinema_back.payment.client;

import com.ps.cinema_back.common.exception.BakongAuthException;
import com.ps.cinema_back.payment.config.BakongProperties;
import com.ps.cinema_back.payment.dto.response.BakongCheckMd5Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
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
        String token = bakongProperties.getBearer().getToken();

        if (token == null || token.isBlank()) {
            log.error("Bakong bearer token is missing/blank — check bakong.bearer.token config. Refusing to call Bakong without auth.");
            throw new BakongAuthException(
                    "Bakong API token is not configured. Payment verification is temporarily unavailable."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

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

        } catch (HttpClientErrorException.Unauthorized e) {

            log.error("Bakong rejected request as Unauthorized (401) for md5={}. Token is likely invalid or expired — renew it via Bakong's token endpoint.", md5Hash);

            throw new BakongAuthException(
                    "Bakong API token was rejected (401 Unauthorized). The token needs to be renewed."
            );

        } catch (HttpClientErrorException e) {

            log.error("Bakong rejected request with client error {} for md5={}: {}",
                    e.getStatusCode(), md5Hash, e.getResponseBodyAsString());
            return null;

        } catch (HttpServerErrorException e) {

            log.error("Bakong server error {} for md5={}: {}",
                    e.getStatusCode(), md5Hash, e.getMessage());
            return null;

        } catch (RestClientException e) {

            log.error("Failed to reach Bakong (network/timeout) for md5={}: {}", md5Hash, e.getMessage());
            return null;
        }
    }
}