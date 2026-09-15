package com.campusiq.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class Esp32DeviceAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String ATTENDANCE_ENDPOINT =
            "/api/fingerprint/attendance";

    private static final String DEVICE_ID_HEADER =
            "X-Device-ID";

    private static final String DEVICE_SECRET_HEADER =
            "X-Device-Secret";

    @Value("${campusiq.esp32.device-id}")
    private String configuredDeviceId;

    @Value("${campusiq.esp32.device-secret}")
    private String configuredDeviceSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        return !ATTENDANCE_ENDPOINT.equals(
                request.getServletPath()
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String deviceId =
                request.getHeader(DEVICE_ID_HEADER);

        String deviceSecret =
                request.getHeader(DEVICE_SECRET_HEADER);

        if (deviceId == null
                || deviceSecret == null
                || !deviceId.equals(configuredDeviceId)
                || !deviceSecret.equals(configuredDeviceSecret)) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    "application/json"
            );

            response.getWriter().write(
                    "{\"message\":\"Invalid ESP32 device credentials\"}"
            );

            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        deviceId,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_DEVICE"
                                )
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(
                request,
                response
        );
    }
}