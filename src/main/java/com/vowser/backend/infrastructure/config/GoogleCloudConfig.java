package com.vowser.backend.infrastructure.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v2.SpeechClient;
import com.google.cloud.speech.v2.SpeechSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Configuration
public class GoogleCloudConfig {

    @Value("${google.cloud.credentials.json}")
    private String credentialsJsonBase64;

    @Value("${gcp.location:global}")
    private String location;

    @Bean
    @ConditionalOnProperty(name = "google.cloud.credentials.json", matchIfMissing = false)
    public SpeechClient speechClient() {
        try {
            byte[] decodedCredentials = Base64.getDecoder().decode(credentialsJsonBase64);
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(decodedCredentials));
            
            FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

            String endpoint = "global".equals(location)
                    ? "speech.googleapis.com:443" 
                    : location + "-speech.googleapis.com:443";

            log.info("Configuring Google Cloud Speech client with endpoint: {}", endpoint);

            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .setEndpoint(endpoint)
                    .build();

            return SpeechClient.create(settings);
            
        } catch (IOException e) {
            log.error("Failed to create Google Cloud Speech client", e);
            throw new RuntimeException("Failed to initialize Google Cloud Speech client", e);
        }
    }
}