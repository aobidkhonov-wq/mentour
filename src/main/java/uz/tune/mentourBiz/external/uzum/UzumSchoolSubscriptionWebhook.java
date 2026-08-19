package uz.tune.mentourBiz.external.uzum;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.external.octo.SchoolSubscriptionPayment;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionPaymentRepo;
import uz.tune.mentourBiz.utils.CoreUtils;

/**
 * Receives the single acquiring callback Uzum Checkout is configured (on the terminal side)
 * to POST after every financial operation. The URL is public (permitted in SecurityConfig).
 *
 * The callback carries no amount, so instead of trusting it directly the payment is
 * confirmed server-to-server via /api/v1/payment/getOrderStatus (status + amount check).
 * Uzum also signs callbacks with an X-Signature header (ECDSA); verification can be added
 * once Uzum provides the public key.
 */
@RestController
@RequestMapping("/api/v1/public/uzum/subscription")
@RequiredArgsConstructor
public class UzumSchoolSubscriptionWebhook {

    private final SchoolSubscriptionPaymentRepo subscriptionPaymentRepo;
    private final UzumPaymentConfirmationService confirmationService;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody UzumCallbackData callback) {
        Logger.logInfo(">> [UzumWebhook] " + callback.getOperationType() + "/" + callback.getOperationState()
                + " for order: " + callback.getOrderNumber());

        SchoolSubscriptionPayment payment = subscriptionPaymentRepo
                .findByShopTransactionId(callback.getOrderNumber())
                .orElse(null);
        if (payment == null) {
            Logger.logWarn("Uzum callback for unknown order: " + callback.getOrderNumber());
            return ResponseEntity.status(404).build();
        }

        if (CoreUtils.isPresent(payment.getUzumTransactionId())
                && !payment.getUzumTransactionId().equals(callback.getOrderId())) {
            Logger.logWarn("Uzum callback orderId mismatch for order " + callback.getOrderNumber()
                    + ": expected " + payment.getUzumTransactionId() + ", got " + callback.getOrderId());
            return ResponseEntity.status(404).build();
        }

        if ("FAIL".equalsIgnoreCase(callback.getOperationState())) {
            if (payment.getStatus() != TransactionStatus.SUCCESS) {
                payment.setStatus(TransactionStatus.FAILED);
                subscriptionPaymentRepo.save(payment);
            }
            return ResponseEntity.ok().build();
        }

        if ("COMPLETE".equalsIgnoreCase(callback.getOperationType())
                || "AUTHORIZE".equalsIgnoreCase(callback.getOperationType())) {
            // the callback carries no amount, so the payment is confirmed straight from Uzum
            UzumPaymentConfirmationService.Outcome outcome =
                    confirmationService.confirm(payment, callback.getOrderId());
            if (outcome == UzumPaymentConfirmationService.Outcome.UNAVAILABLE) {
                // ask Uzum to repeat the callback instead of silently dropping a payment
                // that may well have gone through; the reconciliation job is the second net
                return ResponseEntity.status(500).build();
            }
            return ResponseEntity.ok().build();
        }

        if ("REFUND".equalsIgnoreCase(callback.getOperationType())
                || "REVERSE".equalsIgnoreCase(callback.getOperationType())) {
            payment.setStatus(TransactionStatus.CANCELED);
            subscriptionPaymentRepo.save(payment);
        }

        return ResponseEntity.ok().build();
    }
}
