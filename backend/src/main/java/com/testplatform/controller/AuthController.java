package com.testplatform.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials, Authentication authentication) {
        Map<String, Object> realResponse = new HashMap<>();
        String username = credentials == null ? null : credentials.get("username");
        String password = credentials == null ? null : credentials.get("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            realResponse.put("success", false);
            realResponse.put("message", "Username and password are required.");
            return realResponse;
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                realResponse.put("success", false);
                realResponse.put("message", "Invalid username or password.");
                return realResponse;
            }

            String authorization = "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8)
            );
            realResponse.put("success", true);
            realResponse.put("message", "Login successful.");
            realResponse.put("authorization", authorization);
            realResponse.put("username", userDetails.getUsername());
            return realResponse;
        } catch (UsernameNotFoundException exception) {
            realResponse.put("success", false);
            realResponse.put("message", "Invalid username or password.");
            return realResponse;
        }

    }
}
