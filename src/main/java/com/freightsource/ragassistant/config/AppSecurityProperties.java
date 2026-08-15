package com.freightsource.ragassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(String username, String password) {

    public AppSecurityProperties {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("app.security.username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("app.security.password must not be blank");
        }
    }
}
