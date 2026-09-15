package com.campusiq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.campusiq.security.Esp32DeviceAuthenticationFilter;
import com.campusiq.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(userDetailsService);

        authenticationProvider.setPasswordEncoder(
                passwordEncoder
        );

        return new ProviderManager(
                authenticationProvider
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            Esp32DeviceAuthenticationFilter esp32DeviceAuthenticationFilter)
            throws Exception {

        http
            .csrf(csrf ->
                    csrf.disable()
            )

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            .exceptionHandling(exception ->
                    exception
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.setStatus(
                                                HttpServletResponse.SC_UNAUTHORIZED
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        response.setStatus(
                                                HttpServletResponse.SC_FORBIDDEN
                                        )
                        )
            )

            .authorizeHttpRequests(auth ->
                    auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/fingerprint/attendance"
                        )
                        .hasRole("DEVICE")

                        .requestMatchers(
                                "/api/admin/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/student/**"
                        )
                        .hasRole("STUDENT")

                        .requestMatchers(
                                "/api/proctoring/**"
                        )
                        .hasRole("STUDENT")

                        .requestMatchers(
                                "/api/faculty/**"
                        )
                        .hasRole("FACULTY")

                        .requestMatchers(
                                "/api/tpo/**"
                        )
                        .hasRole("TPO")

                        .requestMatchers(
                                "/api/companies/**"
                        )
                        .hasRole("TPO")

                        .requestMatchers(
                                "/api/eligibility/**"
                        )
                        .hasRole("TPO")

                        .anyRequest()
                        .authenticated()
            )

            .addFilterBefore(
                    esp32DeviceAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            )

            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}