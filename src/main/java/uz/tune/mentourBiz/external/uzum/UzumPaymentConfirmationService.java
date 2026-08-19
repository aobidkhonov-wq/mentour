package uz.tune.mentourBiz.external.uzum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.external.octo.SchoolSubscriptionPayment;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;
import uz.tune.mentourBiz.rest.service.SchoolSubscriptionService;

/**
 * Confirms a Uzum payment server-to-server and extends the subscription when it really was paid.
 * Shared by the callback webhook and the reconciliation job so both verify a payment identically.
 */
@Service
@RequiredArgsConstructor
public class UzumPaymentConfirmationService {

    public enum Outcome {
        // the payment is confirmed as paid and the subscription has been extended
        CONFIRMED,
        // Uzum says the order was not paid (declined, still registered, refunded, wrong amount)
        NOT_PAID,
        // the cross-check itself could not be performed, the payment stays pending
        UNAVAILABLE
    }

    private final ExtUzumService extUzumService;
    private final SchoolSubscriptionService subscriptionService;

    public Outcome confirm(SchoolSubscriptionPayment payment, String uzumOrderId) {
        if (payment.getStatus() == TransactionStatus.SUCCESS) return Outcome.CONFIRMED;

        UzumOrderStatus status = extUzumService.getOrderStatus(uzumOrderId);
        if (status == null) {
            Logger.logWarn("Uzum getOrderStatus unavailable for order: " + uzumOrderId);
            return Outcome.UNAVAILABLE;
        }

        long expectedAmount = extUzumService.toChargedAmount(payment.getTotalAmount(), payment.getPlan().getCurrency());
        Logger.logInfo(">> [UzumConfirm] " + uzumOrderId + " status=" + status.getStatus()
                + " expected=" + expectedAmount + " amount=" + status.getAmount()
                + " totalAmount=" + status.getTotalAmount() + " completedAmount=" + status.getCompletedAmount());

        if (!"COMPLETED".equalsIgnoreCase(status.getStatus())) {
            return Outcome.NOT_PAID;
        }

        // "amount" is the sum we sent at registration, so it is the deterministic one to verify
        // against; totalAmount is Uzum's final sum and may legitimately differ from it
        Long paidAmount = status.getAmount() != null ? status.getAmount() : status.getTotalAmount();
        if (paidAmount == null || expectedAmount != paidAmount) {
            Logger.logWarn("Uzum amount mismatch for order " + payment.getShopTransactionId()
                    + ": expected " + expectedAmount + ", got " + paidAmount);
            return Outcome.NOT_PAID;
        }

        subscriptionService.processSuccessfulPayment(payment.getShopTransactionId());
        return Outcome.CONFIRMED;
    }
}
