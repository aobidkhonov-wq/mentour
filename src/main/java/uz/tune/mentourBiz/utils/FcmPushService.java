package uz.tune.mentourBiz.utils;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.repository.FcmTokenRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final FcmTokenRepo tokenRepo;

    @Transactional
    public void sendPush(List<String> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) return;

        for (int i = 0; i < tokens.size(); i += 500) {
            int end = Math.min(i + 500, tokens.size());
            List<String> chunk = tokens.subList(i, end);

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .addAllTokens(chunk)
                    .build();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                if (response.getFailureCount() > 0) {
                    List<SendResponse> responses = response.getResponses();
                    for (int k = 0; k < responses.size(); k++) {

                        if (!responses.get(k).isSuccessful()) {
                            System.out.println("Failed");
                            Logger.logWarn("FCM Failure for token: " + responses.get(k).getException().getMessage());

                            tokenRepo.deleteByToken(chunk.get(k));
                        }
                    }
                }
            } catch (Exception e) {
                Logger.exception("FCM Chunk Failure", e);
            }
        }
    }
}