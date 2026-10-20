package com.snoozeshare.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.snoozeshare.auth.security.FirebaseAuthFilter;
import com.snoozeshare.auth.security.FirebaseTokenVerifier;
import com.snoozeshare.auth.security.RoleAuthorizationFilter;
import com.snoozeshare.auth.security.RoleAuthorizationService;

/** Stateless bearer-token security for the protected API namespaces. */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
    FirebaseAuthFilter firebaseAuthFilter(FirebaseTokenVerifier verifier) {
        return new FirebaseAuthFilter(verifier);
    }

    @Bean
    @ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
    RoleAuthorizationFilter roleAuthorizationFilter(RoleAuthorizationService authorization) {
        return new RoleAuthorizationFilter(authorization);
    }

    @Bean
    @ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        FirebaseAuthFilter authFilter,
        RoleAuthorizationFilter roleFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> { })
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/auth/**", "/api/guest/**", "/api/host/**", "/api/agent/**")
                    .authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(roleFilter, FirebaseAuthFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain disabledSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
