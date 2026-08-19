package uz.tune.mentourBiz.whatsApp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.enums.Lang;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppProperties props;
    private final RestTemplate restTemplate;

    public void sendTemplateMessage(String toPhone, String templateName, Lang lang, List<String> parameters) {
        if (toPhone == null || toPhone.isBlank()) return;

        // 998901234567)
        String cleanPhone = toPhone.replaceAll("[^0-9]", "");

        // VERIFIED META CODES
        String whatsappLangCode = switch (lang) {
            case RUS -> "ru";
            case UZB -> "uz";
            case TJK -> "tg"; // Tajik
            case KRG -> "ky"; // Kyrgyz
            case ENG -> "en"; // English
            case KAA -> "uz"; // Fallback: Meta does not support 'kaa' we use  'uz'.
            default -> "uz";
        };

        List<WhatsAppTemplateRequest.Parameter> whatsappParams = parameters.stream()
                .map(p -> WhatsAppTemplateRequest.Parameter.builder().text(p).build())
                .collect(Collectors.toList());

        WhatsAppTemplateRequest request = WhatsAppTemplateRequest.builder()
                .to(cleanPhone)
                .template(WhatsAppTemplateRequest.Template.builder()
                        .name(templateName)
                        .language(WhatsAppTemplateRequest.Language.builder().code(whatsappLangCode).build())
                        .components(List.of(WhatsAppTemplateRequest.Component.builder()
                                .parameters(whatsappParams)
                                .build()))
                        .build())
                .build();

        sendViaApi(request);
    }

    private void sendViaApi(WhatsAppTemplateRequest body) {
        try {
            String url = String.format("%s/%s/messages", props.getBaseUrl(), props.getPhoneNumberId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getToken());

            HttpEntity<WhatsAppTemplateRequest> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);
            Logger.logInfo("WhatsApp API Success: Template sent to " + body.getTo());
        } catch (Exception e) {
            Logger.exception("WhatsApp API Failure", e);
        }
    }
}