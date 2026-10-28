package com.example.recruitment.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final TalentBridgeUserDetailsService userDetailsService;

    public SecurityConfig(TalentBridgeUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ----------------------------------------------------------------
            // AUTHORIZATION RULES
            // ----------------------------------------------------------------
            .authorizeHttpRequests(auth -> auth
                // Public assets and pages
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/", "/login", "/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/{id}", "/api/v1/jobs", "/api/v1/jobs/*").permitAll()

                // Recruiter & Admin job management
                .requestMatchers("/jobs/new", "/jobs/*/edit", "/jobs/*/status").hasAnyRole("RECRUITER", "ADMIN")
                .requestMatchers("/recruiter/**").hasAnyRole("RECRUITER", "ADMIN")

                // Applicant apply endpoint
                .requestMatchers("/applications/apply/**").hasRole("APPLICANT")

                // Interviews, Offers, Applications, Notifications, Dashboard
                .requestMatchers("/applications/**").authenticated()
                .requestMatchers("/interviews/**").authenticated()
                .requestMatchers("/offers/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                .requestMatchers("/dashboard/**").authenticated()

                // Admin-only areas
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // REST API endpoints
                .requestMatchers("/api/**").authenticated()

                // Authenticated-only fallback
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})

            // ----------------------------------------------------------------
            // FORM LOGIN
            // ----------------------------------------------------------------
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )

            // ----------------------------------------------------------------
            // LOGOUT
            // ----------------------------------------------------------------
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ----------------------------------------------------------------
            // CSRF — enabled by default for browser forms, ignored for /api/**
            // ----------------------------------------------------------------
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

            // ----------------------------------------------------------------
            // SECURITY HEADERS
            // ----------------------------------------------------------------
            .headers(headers -> headers
                .frameOptions(fo -> fo.sameOrigin())
                .contentTypeOptions(ct -> ct.disable())   // let browser sniff — Spring adds X-Content-Type-Options
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )

            // ----------------------------------------------------------------
            // EXCEPTION HANDLING
            // ----------------------------------------------------------------
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}
