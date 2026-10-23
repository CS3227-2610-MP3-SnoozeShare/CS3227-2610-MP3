package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.snoozeshare.auth.security.FirebaseAdminTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;

/** Verifies the Firebase token adapter without network calls. */
class FirebaseTokenVerifierTest {

    private static final String PROJECT_ID = "snoozeshare-test";
    private static final String ISSUER = "https://securetoken.google.com/" + PROJECT_ID;

    @Test
    void acceptsVerifiedTokenFromExpectedProject() throws VerificationException, FirebaseAuthException {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(auth.verifyIdToken("token", true)).thenReturn(token);
        when(token.getUid()).thenReturn("firebase-uid");
        when(token.getEmail()).thenReturn("user@example.com");
        when(token.isEmailVerified()).thenReturn(true);
        when(token.getIssuer()).thenReturn(ISSUER);

        var result = new FirebaseAdminTokenVerifier(auth, PROJECT_ID).verify("token");

        assertThat(result.uid()).isEqualTo("firebase-uid");
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.emailVerified()).isTrue();
    }

    @Test
    void rejectsTokenFromWrongProject() throws FirebaseAuthException {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(auth.verifyIdToken("token", true)).thenReturn(token);
        when(token.getIssuer()).thenReturn("https://securetoken.google.com/other-project");

        assertThatThrownBy(() -> new FirebaseAdminTokenVerifier(auth, PROJECT_ID).verify("token"))
            .isInstanceOf(VerificationException.class)
            .extracting(exception -> ((VerificationException) exception).failure())
            .isEqualTo(VerificationFailure.INVALID_TOKEN);
    }

    @Test
    void classifiesInvalidAndUnavailableProviderResponses() throws FirebaseAuthException {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseAuthException invalid = mock(FirebaseAuthException.class);
        FirebaseAuthException unavailable = mock(FirebaseAuthException.class);
        when(unavailable.getErrorCode()).thenReturn(ErrorCode.UNAVAILABLE);
        when(auth.verifyIdToken("invalid", true)).thenThrow(invalid);
        when(auth.verifyIdToken("unavailable", true)).thenThrow(unavailable);
        var verifier = new FirebaseAdminTokenVerifier(auth, PROJECT_ID);

        assertThatThrownBy(() -> verifier.verify("invalid"))
            .isInstanceOf(VerificationException.class)
            .extracting(exception -> ((VerificationException) exception).failure())
            .isEqualTo(VerificationFailure.INVALID_TOKEN);
        assertThatThrownBy(() -> verifier.verify("unavailable"))
            .isInstanceOf(VerificationException.class)
            .extracting(exception -> ((VerificationException) exception).failure())
            .isEqualTo(VerificationFailure.PROVIDER_UNAVAILABLE);
    }
}
