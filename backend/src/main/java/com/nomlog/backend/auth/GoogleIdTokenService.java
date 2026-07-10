package com.nomlog.backend.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import java.security.GeneralSecurityException;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdTokenService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenService(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidGoogleTokenException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            // GoogleIdTokenVerifier#verify throws IllegalArgumentException (rather than
            // returning null) when the input isn't even well-formed JWT (e.g. no dots).
            throw new InvalidGoogleTokenException("Failed to verify Google ID token");
        }
    }
}
