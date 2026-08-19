package uz.tune.mentourBiz.rest.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramAlertService {

    @Value("${app.telegram.alert-bot-token}")
    private String botToken;

    @Value("${app.telegram.alert-chat-id}")
    private String alertChatId;

    @Value("${app.telegram.alert-thread-id}")
    private String alertThreadId;

    private static final ZoneId TZ = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestTemplate restTemplate = new RestTemplate();

    public void notifyFrozenSchool(User user, String schoolName) {
        String text = """
                🏫 <b>Frozen school login attempt</b>
                👤 Name: %s %s
                🔑 Username: <code>%s</code>
                📞 Phone: %s
                🎭 Role: %s
                🏫 School: %s
                🕐 Time: %s
                """.formatted(
                user.getFirstName(), user.getLastName(),
                user.getUsername(),
                user.getPhoneNumber() != null ? user.getPhoneNumber() : "—",
                user.getRole(),
                schoolName != null ? schoolName : "—",
                ZonedDateTime.now(TZ).format(FMT)
        );
        send(text);
    }

    public void notifyExpiredSubscription(User user, String schoolName, java.time.Instant expiresAt) {
        String expired = ZonedDateTime.ofInstant(expiresAt, TZ).format(FMT);
        String text = """
                ⚠️ <b>Expired subscription login attempt</b>
                👤 Name: %s %s
                🔑 Username: <code>%s</code>
                📞 Phone: %s
                🎭 Role: %s
                🏫 School: %s
                📅 Expired at: %s
                🕐 Time: %s
                """.formatted(
                user.getFirstName(), user.getLastName(),
                user.getUsername(),
                user.getPhoneNumber() != null ? user.getPhoneNumber() : "—",
                user.getRole(),
                schoolName != null ? schoolName : "—",
                expired,
                ZonedDateTime.now(TZ).format(FMT)
        );
        send(text);
    }

    public void notifyDeletedOrganization(User user, String orgName) {
        String text = """
                🏛 <b>Deleted organization login attempt</b>
                👤 Name: %s %s
                🔑 Username: <code>%s</code>
                📞 Phone: %s
                🎭 Role: %s
                🏛 Organization: %s
                🕐 Time: %s
                """.formatted(
                user.getFirstName(), user.getLastName(),
                user.getUsername(),
                user.getPhoneNumber() != null ? user.getPhoneNumber() : "—",
                user.getRole(),
                orgName != null ? orgName : "—",
                ZonedDateTime.now(TZ).format(FMT)
        );
        send(text);
    }

    private void send(String text) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", alertChatId);
            body.put("message_thread_id", Integer.parseInt(alertThreadId));
            body.put("text", text);
            body.put("parse_mode", "HTML");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("Failed to send Telegram alert: {}", e.getMessage());
        }
    }
}
