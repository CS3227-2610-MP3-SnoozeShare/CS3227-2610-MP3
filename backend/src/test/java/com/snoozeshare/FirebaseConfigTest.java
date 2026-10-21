package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.snoozeshare.auth.config.FirebaseConfig;

/** Verifies Firebase configuration rejects incomplete startup settings. */
class FirebaseConfigTest {

    @Test
    void rejectsBlankProjectId() {
        var config = new FirebaseConfig("", "host-code", "agent-code");

        assertThatThrownBy(config::firebaseApp)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("FIREBASE_PROJECT_ID");
    }

    @Test
    void rejectsBlankRegistrationCodes() {
        var config = new FirebaseConfig("project", "", "agent-code");

        assertThatThrownBy(config::firebaseApp)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SNOOZESHARE_HOST_REGISTRATION_CODE");
    }
}
