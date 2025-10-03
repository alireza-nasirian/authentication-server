package com.example.authserver.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthService.class);

    @Value("${app.oauth2.google.clientId}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifier getVerifier() {
        if (verifier == null) {
            verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        }
        return verifier;
    }

    public GoogleUserInfo verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdToken idToken = getVerifier().verify(idTokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                String userId = payload.getSubject();
                String email = payload.getEmail();
                boolean emailVerified = payload.getEmailVerified();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String familyName = (String) payload.get("family_name");
                String givenName = (String) payload.get("given_name");

                if (!emailVerified) {
                    logger.warn("Email not verified for Google user: {}", email);
                    return null;
                }

                return new GoogleUserInfo(userId, email, name, pictureUrl, givenName, familyName);
            } else {
                logger.error("Invalid Google ID token");
                return null;
            }
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Error verifying Google ID token", e);
            return null;
        }
    }

    public record GoogleUserInfo(String googleId, String email, String name, String pictureUrl, String givenName,
                                 String familyName) {
    }
}
