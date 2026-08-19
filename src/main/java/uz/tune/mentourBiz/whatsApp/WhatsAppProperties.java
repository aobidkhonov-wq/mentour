package uz.tune.mentourBiz.whatsApp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {
    private String baseUrl = "https://graph.facebook.com/v21.0";
    private String token;
    private String phoneNumberId;
    private String verifyToken;
}