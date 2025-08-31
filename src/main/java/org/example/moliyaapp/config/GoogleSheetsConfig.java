package org.example.moliyaapp.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
@ConditionalOnProperty(name = "google.sheets.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class GoogleSheetsConfig {

    @Value("${google.sheets.credentials.path}")
    private String credentialsPath;

    @Value("${google.sheets.application.name}")
    private String applicationName;

    @Bean
    public Sheets sheetsService() throws IOException, GeneralSecurityException {
        log.info("Initializing Google Sheets service with credentials: {}", credentialsPath);

        GoogleCredentials credentials;

        try {
            InputStream credentialsStream;
            if (credentialsPath.startsWith("classpath:")) {
                String resourcePath = credentialsPath.substring("classpath:".length());
                credentialsStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (credentialsStream == null) {
                    throw new FileNotFoundException("Credentials file not found in classpath: " + resourcePath);
                }
            } else {
                credentialsStream = new FileInputStream(credentialsPath);
            }

            credentials = GoogleCredentials
                    .fromStream(credentialsStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        } catch (IOException e) {
            log.error("Failed to load Google Sheets credentials from: {}", credentialsPath, e);
            throw e;
        }

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        return new Sheets.Builder(httpTransport, jsonFactory,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }
}