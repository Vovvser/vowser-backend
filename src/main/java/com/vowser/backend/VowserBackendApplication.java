package com.vowser.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Vowser Backend Application
 * 
 * The Central Communication & Control Hub for the Vowser Ecosystem.
 * Acts as an API gateway and real-time control hub connecting
 * vowser-client to vowser-mcp-server.
 */
@SpringBootApplication
@EnableFeignClients
public class VowserBackendApplication {

    public static void main(String[] args) {
        loadEnvironmentVariables();
        SpringApplication.run(VowserBackendApplication.class, args);
    }

    private static void loadEnvironmentVariables() {
        loadEnvFile(".env");
        loadEnvFile(".env.local");
    }

    private static void loadEnvFile(String fileName) {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .filename(fileName)
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }
}