package com.muklog.backend.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.muklog.backend.user.User;
import com.muklog.backend.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final GoogleIdTokenService googleIdTokenService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
        GoogleIdTokenService googleIdTokenService,
        UserRepository userRepository,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.googleIdTokenService = googleIdTokenService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String loginWithGoogle(String idToken) {
        GoogleIdToken.Payload payload = googleIdTokenService.verify(idToken);

        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByGoogleSub(googleSub)
            .orElseGet(() -> userRepository.save(new User(googleSub, email, name)));

        return jwtTokenProvider.createToken(user.getId());
    }
}
