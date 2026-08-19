package uz.tune.mentourBiz.config;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.repository.RegionRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private final SchoolRepo schoolRepo;
    private final RegionRepository regionRepository;

    public FirebaseConfig(SchoolRepo schoolRepo, RegionRepository regionRepository) {
        this.schoolRepo = schoolRepo;
        this.regionRepository = regionRepository;
    }

    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ClassPathResource("serviceAccountKey.json").getInputStream()))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                Logger.logInfo("Firebase Admin SDK initialized successfully.");
            }
        } catch (IOException e) {
            Logger.exception("Failed to initialize Firebase Admin SDK. Push notifications will not work.", e);
        }
    }

}