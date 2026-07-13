package com.muklog.backend.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleIdTokenVerifierConfig {

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(@Value("${app.google.client-id}") String clientId) {
        // .env.properties is imported as optional, so a wrong working directory
        // silently leaves this blank and every token then fails the audience
        // check with an opaque 401. Fail loudly at startup instead.
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException(
                "app.google.client-id is not set. Set GOOGLE_CLIENT_ID "
                    + "(e.g. via backend/.env.properties) before starting the app.");
        }
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(clientId))
            .build();
    }
}
