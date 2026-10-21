package com.snoozeshare.auth.security;

/** Boundary for verifying Firebase ID tokens. */
public interface FirebaseTokenVerifier {

    FirebaseAuthentication verify(String idToken) throws VerificationException;

    record FirebaseAuthentication(String uid, String email, boolean emailVerified) { }

    enum VerificationFailure {
        INVALID_TOKEN,
        PROVIDER_UNAVAILABLE
    }

    /** Safe application exception that does not expose the submitted token. */
    class VerificationException extends Exception {

        private final VerificationFailure failure;

        public VerificationException(VerificationFailure failure, Throwable cause) {
            super(failure.name(), cause);
            this.failure = failure;
        }

        public VerificationFailure failure() {
            return failure;
        }
    }
}
