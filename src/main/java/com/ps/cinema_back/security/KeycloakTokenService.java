package com.ps.cinema_back.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class KeycloakTokenService {

    @Value("${keycloak.issuer-uri}")
    private String issuerUri;

    private JwtDecoder jwtDecoder;

    private JwtDecoder getDecoder() {
        // Lazily built once — fetches Keycloak's public signing keys (JWKS)
        // from the issuer and caches them. This is how we PROVE the token
        // really came from our Keycloak server and wasn't forged.
        if (jwtDecoder == null) {
            jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        }
        return jwtDecoder;
    }

    /**
     * Verifies a Keycloak-issued access token and returns its claims.
     * Throws if the signature/issuer/expiry don't check out.
     */
    public Jwt verifyAndDecode(String keycloakAccessToken) {
        return getDecoder().decode(keycloakAccessToken);
    }
}