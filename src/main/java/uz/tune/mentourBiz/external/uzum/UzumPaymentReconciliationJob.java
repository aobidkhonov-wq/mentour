package uz.tune.mentourBiz.external.uzum;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.external.octo.SchoolSubscriptionPayment;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionPaymentRepo;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Uzum delivers the acquiring callback only to the URL configured on the terminal by Uzum itself,
 * so a callback that is missing or lost leaves a genuinely paid subscription unextended. Pending
 * Uzum payments are therefore also confirmed by polling getOrderStatus directly.
 */
@Component
@RequiredArgsConstructor
public class UzumPaymentReconciliationJob {

    private static final Duration LOOKBACK = Duration.ofHours(24);

    private final SchoolSubscriptionPaymentRepo paymentRepo;
    private final UzumPaymentConfirmationService confirmationService;

    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void confirmPendingPayments() {
        List<SchoolSubscriptionPayment> pending = paymentRepo
                .findAllByStatusAndUzumTransactionIdNotNullAndCreatedAtAfter(
                        TransactionStatus.PENDING, Instant.now().minus(LOOKBACK));
        if (pending.isEmpty()) return;

        Logger.logInfo(">> [UzumReconcile] checking " + pending.size() + " pending Uzum payment(s)");

        for (SchoolSubscriptionPayment payment : pending) {
            try {
                UzumPaymentConfirmationService.Outcome outcome =
                        confirmationService.confirm(payment, payment.getUzumTransactionId());
                if (outcome == UzumPaymentConfirmationService.Outcome.CONFIRMED) {
                    Logger.logInfo(">> [UzumReconcile] recovered missed payment: " + payment.getShopTransactionId());
                }
            } catch (Throwable th) {
                // one broken payment must not stop the rest of the batch
                Logger.exception("Uzum reconciliation failed for order " + payment.getShopTransactionId(), th);
            }
        }
    }
}
