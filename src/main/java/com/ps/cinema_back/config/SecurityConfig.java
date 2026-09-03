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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. MUST ENABLE CORS TO PICK UP YOUR CorsConfig BEAN
                .cors(Customizer.withDefaults())

                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 2. MUST ALLOW PREFLIGHT OPTIONS REQUESTS GLOBALLY
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 3. PUBLIC AUTH & STATIC ENDPOINTS
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/uploads/**"
                        ).permitAll()

                        // 4. PUBLIC BROWSING (Allow unauthenticated visitors to view homepage)
                        .requestMatchers(HttpMethod.GET, "/api/v1/genres/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/movies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/cinemas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/halls/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/seats/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/showtimes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/showtime/*/layout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/verify-bakong/**").permitAll()

                        // 5. GENRE MANAGEMENT (Staff & Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/genres/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/genres/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/genres/**").hasAnyRole("STAFF", "ADMIN")

                        // 6. MOVIE MANAGEMENT (Staff & Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/movies/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/movies/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/movies/**").hasAnyRole("STAFF", "ADMIN")

                        // 7. CINEMA, HALL, SEAT, SHOWTIME MANAGEMENT (Staff & Admin)
                        .requestMatchers(HttpMethod.POST, "/api/v1/cinemas/**", "/api/v1/halls/**", "/api/v1/seats/**", "/api/v1/showtimes/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/cinemas/**", "/api/v1/halls/**", "/api/v1/seats/**", "/api/v1/showtimes/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/cinemas/**", "/api/v1/halls/**", "/api/v1/seats/**", "/api/v1/showtimes/**").hasAnyRole("STAFF", "ADMIN")

                        // 8. ADMIN-ONLY OPERATIONS & DESTRUCTION
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/trash").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/restore").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/*/hard").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/payments/*/refund").hasRole("ADMIN")

                        // 9. USER MANAGEMENT
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/staff").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/*/toggle-status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/*").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/page").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                        // 10. BOOKING & SCANNER
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/my-bookings").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/{id}").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/number/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/cancel").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets/scan/**").hasAnyRole("STAFF", "ADMIN")

                        // 11. PAYMENTS
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/khqr/generate").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/booking/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/transaction/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/{id}").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments").hasAnyRole("STAFF", "ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}