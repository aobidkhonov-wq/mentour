package uz.tune.mentourBiz.external.octo;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uz.tune.mentourBiz.config.logger.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExtOctoService {

    private final RestTemplate restTemplate;

    @Value("${app.octo.shop-id}")
    private Long shopId;

    @Value("${app.octo.secret}")
    private String secret;

    @Value("${app.backend-url}")
    private String backendUrl;

    public Map<String, Object> preparePayment(SchoolSubscriptionPayment payment) {
        String url = "https://secure.octo.uz/prepare_payment";
        String initTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, Object> request = new HashMap<>();
        request.put("octo_shop_id", shopId);
        request.put("octo_secret", secret);
        request.put("shop_transaction_id", payment.getShopTransactionId());
        request.put("auto_capture", true);
        request.put("test", true);
        request.put("init_time", initTime);
        request.put("total_sum", payment.getTotalAmount());
        request.put("currency", payment.getPlan().getCurrency().name());
        request.put("description", "Subscription for school " + payment.getSchool().getName());
        request.put("notify_url", backendUrl + "/api/v1/public/octo/webhook");
        request.put("language", "ru");

        Map<String, String> userData = new HashMap<>();
        userData.put("user_id", payment.getSchool().getUuid().toString());
        userData.put("email", "support@mentour.uz");
        userData.put("phone", "998901234567");
        request.put("user_data", userData);

        try {
            Logger.logInfo(">> Attempting OCTO prepare_payment. ShopID: " + shopId);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null) {
                String errorCode = String.valueOf(body.get("error"));
                if ("0".equals(errorCode)) {
                    return (Map<String, Object>) body.get("data");
                } else {
                    Logger.logWarn("OCTO Logical Error Code [" + errorCode + "]: " + body.get("errMessage"));
                }
            }
        } catch (HttpStatusCodeException e) {
            Logger.logWarn("OCTO Server returned HTTP " + e.getStatusCode() + ". Body: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            Logger.exception("Critical error communicating with OCTO", e);
        }
        return null;
    }
}
