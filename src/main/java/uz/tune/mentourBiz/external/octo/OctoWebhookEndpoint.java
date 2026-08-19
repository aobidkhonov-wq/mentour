package uz.tune.mentourBiz.external.octo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.service.impl.SchoolSubscriptionServiceImpl;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/octo")
@RequiredArgsConstructor
public class OctoWebhookEndpoint {

    private final SchoolSubscriptionServiceImpl subscriptionService;

    @PostMapping("/webhook")
    public ResponseEntity<ResponseMessage> handleOctoCallback(@RequestBody Map<String, Object> payload) {
        String status = (String) payload.get("status");
        String shopTransactionId = (String) payload.get("shop_transaction_id");

        if ("succeeded".equalsIgnoreCase(status)) {
            subscriptionService.processSuccessfulPayment(shopTransactionId);
        }

        return ResponseEntity.ok(new ResponseMessage("OK"));
    }
}