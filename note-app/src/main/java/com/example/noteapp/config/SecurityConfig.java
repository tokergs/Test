package com.example.noteapp.config;

import com.example.noteapp.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT → без сессий
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // публичные маршруты
                        .requestMatchers(
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout",
                                "/users/register",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()
                        // всё остальное требует токен
                        .anyRequest().authenticated()
                )
                .userDetailsService(userDetailsService)

                // ВАЖНО: Security Headers
                .headers(headers -> headers
                                // 1. X-Content-Type-Options: nosniff (2.1 из лабы)
                                .contentTypeOptions(contentType -> {})
                                // 2. X-Frame-Options: SAMEORIGIN (2.2 из лабы)
                                .frameOptions(frame -> frame.sameOrigin())

                                // 3. Content Security Policy (2.3 из лабы)
                                .contentSecurityPolicy(csp -> csp
                                        .policyDirectives(
                                                "default-src 'self'; " +
                                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                                        "style-src 'self' 'unsafe-inline'; " +
                                                        "img-src 'self' data:; " +
                                                        "font-src 'self' data:; " +
                                                        "frame-src 'self'"
                                        )
                                )

                                // 4. Referrer Policy (2.4 из лабы)
                                .referrerPolicy(referrer -> referrer
                                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                                )

                )

                // Подключаем JWT фильтр
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}