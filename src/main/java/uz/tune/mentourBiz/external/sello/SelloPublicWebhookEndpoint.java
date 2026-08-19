package uz.tune.mentourBiz.external.sello;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.external.octo.SchoolSubscriptionPayment;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionPaymentRepo;
import uz.tune.mentourBiz.rest.service.SchoolSubscriptionService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/sello")
@RequiredArgsConstructor
public class SelloPublicWebhookEndpoint {

    private final SchoolSubscriptionPaymentRepo paymentRepo;
    private final SchoolSubscriptionService subscriptionService;

    @PostMapping("/check-invoice")
    public ResponseEntity<Void> checkInvoice(@RequestBody Map<String, Object> request) {
        Logger.logInfo(">>> [SELLO CALLBACK] Received check-invoice request");
        Logger.logInfo(">>> Body: " + request.toString());

        try {
            String orderId = (String) request.get("orderId");
            Object amountObj = request.get("amount");

            if (orderId == null || amountObj == null) {
                Logger.logWarn(">>> [SELLO ERROR] Missing orderId or amount in payload");
                return ResponseEntity.badRequest().build();
            }

            Long amountFromSello = (amountObj instanceof Number) ?
                    ((Number) amountObj).longValue() : Long.parseLong(amountObj.toString());

            SchoolSubscriptionPayment payment = paymentRepo.findByShopTransactionId(orderId)
                    .orElseThrow(() -> {
                        Logger.logWarn(">>> [SELLO ERROR] Order not found in DB: " + orderId);
                        return new EntityNotFoundException(MessageKey.SHOP_ORDER_NOT_FOUND.getKey());
                    });

            long dbAmountInTiyins = payment.getTotalAmount() * 100;

            Logger.logInfo(">>> Comparing: Sello[" + amountFromSello + "] vs DB[" + dbAmountInTiyins + "]");

            if (dbAmountInTiyins == amountFromSello.longValue()) {
                Logger.logInfo(">>> [SELLO SUCCESS] Invoice validated.");
                return ResponseEntity.ok().build();
            } else {
                Logger.logWarn(">>> [SELLO ERROR] Amount mismatch! Check Math.");
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            Logger.exception(">>> [SELLO CRITICAL] Webhook processing failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/pay-info")
    public ResponseEntity<Void> payInfo(@RequestBody Map<String, Object> request) {
        String status = (String) request.get("status");
        String orderId = (String) request.get("orderId");
        String ofdUrl = (String) request.get("ofdUrl");

        if ("SUCCESS".equalsIgnoreCase(status)) {
            paymentRepo.findByShopTransactionId(orderId).ifPresent(p -> {
                p.setOfdUrl(ofdUrl);
                paymentRepo.save(p);
            });

            subscriptionService.processSuccessfulPayment(orderId);
        }

        return ResponseEntity.ok().build();
    }
}
