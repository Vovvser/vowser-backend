package com.vowser.backend.infrastructure.config

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v2.SpeechClient
import com.google.cloud.speech.v2.SpeechSettings
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.util.Base64

@Configuration
class GoogleCloudConfig(
    @Value("\${google.cloud.credentials.json}") private val credentialsJsonBase64: String,
    @Value("\${gcp.location}") private val location: String
) {
    @Bean
    fun speechClient(): SpeechClient {
        val decodedCredentials = Base64.getDecoder().decode(credentialsJsonBase64)
        val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(decodedCredentials))
        val credentialsProvider = FixedCredentialsProvider.create(credentials)

        val endpoint = if (location == "global") "speech.googleapis.com:443"
        else "$location-speech.googleapis.com:443"

        val settings = SpeechSettings.newBuilder()
            .setCredentialsProvider(credentialsProvider)
            .setEndpoint(endpoint)
            .build()

        return SpeechClient.create(settings)
    }
}