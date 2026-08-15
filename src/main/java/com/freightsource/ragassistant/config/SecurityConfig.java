package com.freightsource.ragassistant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every endpoint -- including Swagger UI and the web UI -- requires HTTP
 * Basic auth. Credentials come from app.security.* (APP_SECURITY_USERNAME /
 * APP_SECURITY_PASSWORD env vars); the defaults below are for local
 * development only and log a warning if still in use.
 */
@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String DEFAULT_PASSWORD = "changeme";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                // Stateless, credential-per-request API (no session cookie, no
                // browser form submission) -- CSRF protection targets a different
                // threat model and doesn't apply here.
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(AppSecurityProperties props, PasswordEncoder encoder) {
        if (DEFAULT_PASSWORD.equals(props.password())) {
            log.warn("app.security.password is still the local-development default ('{}'). "
                    + "Set APP_SECURITY_PASSWORD before running this anywhere beyond a laptop.", DEFAULT_PASSWORD);
        }
        UserDetails user = User.withUsername(props.username())
                .password(encoder.encode(props.password()))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
