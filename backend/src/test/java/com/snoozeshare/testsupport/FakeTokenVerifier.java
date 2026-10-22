package com.snoozeshare.testsupport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.snoozeshare.auth.security.FirebaseTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;

/** Deterministic token verifier shared by authenticated API integration tests. */
public class FakeTokenVerifier implements FirebaseTokenVerifier {

    private final Map<String, FirebaseAuthentication> tokens = new ConcurrentHashMap<>();

    public void add(String token, String uid, String email) {
        tokens.put(token, new FirebaseAuthentication(uid, email, true));
    }

    public void clear() {
        tokens.clear();
    }

    @Override
    public FirebaseAuthentication verify(String idToken) throws VerificationException {
        FirebaseAuthentication authentication = tokens.get(idToken);
        if (authentication == null) {
            throw new VerificationException(VerificationFailure.INVALID_TOKEN, null);
        }
        return authentication;
    }
}
