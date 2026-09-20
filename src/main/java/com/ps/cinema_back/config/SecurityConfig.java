package com.ps.cinema_back.config;

import com.ps.cinema_back.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                        // 1. Enable CORS by explicitly binding the source here
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                        // 2. Disable CSRF because this is a stateless REST API
                        .csrf(AbstractHttpConfigurer::disable)

                        // 3. Stateless authentication
                        .sessionManagement(session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        )

                        .authorizeHttpRequests(auth -> auth

                                // =========================================================
                                // PREFLIGHT
                                // =========================================================
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // =========================================================
                                // PUBLIC AUTH / SWAGGER / UPLOADS
                                // =========================================================
                                .requestMatchers(
                                        "/api/v1/auth/**",
                                        "/api/v1/settings/public",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/api-docs/**",
                                        "/uploads/**"
                                ).permitAll()

                                // =========================================================
                                // PUBLIC BROWSING
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/genres/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/movies/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/cinemas/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/halls/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/seats/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/showtimes/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/bookings/showtime/*/layout"
                                ).permitAll()

                                // =========================================================
                                // BAKONG PAYMENT VERIFICATION
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/payments/verify-bakong/**"
                                ).permitAll()

                                // =========================================================
                                // PUBLIC F&B
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/concessions/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/concession-categories/**"
                                ).permitAll()

                                // =========================================================
                                // PUBLIC REVIEWS / VOUCHER
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/reviews/movie/**"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/vouchers/apply"
                                ).permitAll()

                                // =========================================================
                                // GENRE MANAGEMENT
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/genres/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/genres/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/genres/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                // =========================================================
                                // MOVIE MANAGEMENT
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/movies/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/movies/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/movies/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                // =========================================================
                                // CINEMA / HALL / SEAT / SHOWTIME MANAGEMENT
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/cinemas/**",
                                        "/api/v1/halls/**",
                                        "/api/v1/seats/**",
                                        "/api/v1/showtimes/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/cinemas/**",
                                        "/api/v1/halls/**",
                                        "/api/v1/seats/**",
                                        "/api/v1/showtimes/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/cinemas/**",
                                        "/api/v1/halls/**",
                                        "/api/v1/seats/**",
                                        "/api/v1/showtimes/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                // =========================================================
                                // CONCESSION MANAGEMENT
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/concessions/**",
                                        "/api/v1/concession-categories/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/concessions/**",
                                        "/api/v1/concession-categories/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/concessions/**",
                                        "/api/v1/concession-categories/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                // =========================================================
                                // ADMIN
                                // =========================================================
                                .requestMatchers(
                                        "/api/v1/admin/reviews/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                .requestMatchers(
                                        "/api/v1/admin/analytics/popularity/**"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        "/api/v1/admin/vouchers/**"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        "/api/v1/admin/**"
                                ).hasRole("ADMIN")

                                // =========================================================
                                // USER TRASH
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/trash"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/users/*/restore"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/users/*/hard"
                                ).hasRole("ADMIN")

                                // =========================================================
                                // REFUND
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/payments/*/refund"
                                ).hasRole("ADMIN")

                                // =========================================================
                                // USER MANAGEMENT
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/users/staff"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/users/*/toggle-status"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/users/*"
                                ).hasAnyRole("ADMIN", "STAFF")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/page"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/*"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/users/*"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                // =========================================================
                                // BOOKING
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/bookings"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/bookings/my-bookings"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/bookings/{id}"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/bookings/number/**"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/bookings/*/cancel"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/bookings"
                                ).hasAnyRole("STAFF", "ADMIN")

                                // =========================================================
                                // TICKET SCANNER
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/tickets/scan/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                // =========================================================
                                // PAYMENTS
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/payments/khqr/generate"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/payments/booking/**"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/payments/transaction/**"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/payments/{id}"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/payments"
                                ).hasAnyRole("STAFF", "ADMIN")

                                // =========================================================
                                // FAVORITES
                                // =========================================================
                                .requestMatchers(
                                        "/api/v1/favorites/**"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                // =========================================================
                                // REVIEWS
                                // =========================================================
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/reviews"
                                ).hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                                // =========================================================
                                // NOTIFICATIONS
                                // =========================================================
                                .requestMatchers(
                                        "/api/v1/notifications/**"
                                ).hasAnyRole("STAFF", "ADMIN")

                                // =========================================================
                                // EVERYTHING ELSE REQUIRES AUTHENTICATION
                                // =========================================================
                                .anyRequest().authenticated()
                        )

                        // JWT authentication
                        .addFilterBefore(
                                jwtAuthFilter,
                                UsernamePasswordAuthenticationFilter.class
                        );

                return http.build();
        }

        // CORS Configuration Bean
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Allow local and production Vercel frontend origins
                configuration.setAllowedOrigins(Arrays.asList(
                        "http://localhost:3000",
                        "https://cinema-frontend-py8v.vercel.app"
                ));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
                configuration.setAllowCredentials(true);
                configuration.setExposedHeaders(List.of("Authorization"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}