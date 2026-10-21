package com.snoozeshare.auth.security;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;

/** Firebase Admin implementation with revocation checking and project validation. */
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private static final String ISSUER_PREFIX = "https://securetoken.google.com/";

    private final FirebaseAuth firebaseAuth;
    private final String expectedProjectId;

    public FirebaseAdminTokenVerifier(FirebaseAuth firebaseAuth, String expectedProjectId) {
        this.firebaseAuth = firebaseAuth;
        this.expectedProjectId = expectedProjectId;
    }

    @Override
    public FirebaseAuthentication verify(String idToken) throws VerificationException {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);
            if (!isExpectedProject(token) || token.getUid() == null || token.getEmail() == null) {
                throw invalidToken();
            }
            return new FirebaseAuthentication(token.getUid(), token.getEmail(), token.isEmailVerified());
        } catch (FirebaseAuthException exception) {
            VerificationFailure failure = isProviderUnavailable(exception)
                ? VerificationFailure.PROVIDER_UNAVAILABLE
                : VerificationFailure.INVALID_TOKEN;
            throw new VerificationException(failure, exception);
        }
    }

    private boolean isExpectedProject(FirebaseToken token) {
        return (ISSUER_PREFIX + expectedProjectId).equals(token.getIssuer());
    }

    private boolean isProviderUnavailable(FirebaseAuthException exception) {
        return exception.getErrorCode() == ErrorCode.UNAVAILABLE
            || exception.getErrorCode() == ErrorCode.DEADLINE_EXCEEDED;
    }

    private VerificationException invalidToken() {
        return new VerificationException(VerificationFailure.INVALID_TOKEN, null);
    }
}
