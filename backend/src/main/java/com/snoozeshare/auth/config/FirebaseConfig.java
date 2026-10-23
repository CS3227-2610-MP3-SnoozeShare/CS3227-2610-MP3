package com.snoozeshare.auth.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.snoozeshare.auth.security.FirebaseAdminTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier;

/** Initializes Firebase Admin through ADC when Firebase-backed auth is enabled. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "snoozeshare.firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private final String projectId;
    private final String hostRegistrationCode;
    private final String agentRegistrationCode;

    public FirebaseConfig(
        @Value("${snoozeshare.firebase.project-id:}") String projectId,
        @Value("${snoozeshare.registration.host-code:}") String hostRegistrationCode,
        @Value("${snoozeshare.registration.agent-code:}") String agentRegistrationCode) {
        this.projectId = projectId;
        this.hostRegistrationCode = hostRegistrationCode;
        this.agentRegistrationCode = agentRegistrationCode;
    }

    @Bean
    public FirebaseApp firebaseApp() {
        requireNonBlank(projectId, "FIREBASE_PROJECT_ID");
        requireNonBlank(hostRegistrationCode, "SNOOZESHARE_HOST_REGISTRATION_CODE");
        requireNonBlank(agentRegistrationCode, "SNOOZESHARE_AGENT_REGISTRATION_CODE");
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(projectId)
                .build();
            return FirebaseApp.initializeApp(options, "snoozeshare");
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase Admin credentials are unavailable", exception);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public FirebaseTokenVerifier firebaseTokenVerifier(FirebaseAuth firebaseAuth) {
        return new FirebaseAdminTokenVerifier(firebaseAuth, projectId);
    }

    private void requireNonBlank(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " must be configured when Firebase auth is enabled");
        }
    }
}
