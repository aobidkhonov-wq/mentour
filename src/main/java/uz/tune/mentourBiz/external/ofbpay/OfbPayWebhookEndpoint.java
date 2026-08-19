package uz.tune.mentourBiz.external.ofbpay;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.external.octo.SchoolSubscriptionPayment;
import uz.tune.mentourBiz.external.ofbpay.payload.ReqOfbPayWebhook;
import uz.tune.mentourBiz.external.ofbpay.payload.ResOfbPayWebhook;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionPaymentRepo;
import uz.tune.mentourBiz.rest.service.SchoolSubscriptionService;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/public/ofb-pay")
@RequiredArgsConstructor
public class OfbPayWebhookEndpoint {

    private final SchoolSubscriptionPaymentRepo paymentRepo;
    private final SchoolSubscriptionService subscriptionService;
    private final ExtOfbPayService extOfbPayService;

    // OFB gateway sends all callbacks to a single endpoint URL; the operation is carried in the "method" field
    @PostMapping
    @PreAuthorize("hasAuthority('EXTERNAL_OFB_PAY')")
    public ResponseEntity<ResOfbPayWebhook> dispatch(@RequestBody ReqOfbPayWebhook request) {
        String method = request.getMethod() == null ? "" : request.getMethod().trim().toLowerCase();
        return switch (method) {
            case "confirm" -> confirm(request);
            case "accept" -> accept(request);
            case "reverse" -> reverse(request);
            default -> {
                Logger.logWarn(">>> [OFB_PAY] unknown method: " + request.getMethod());
                yield ResponseEntity.ok(ResOfbPayWebhook.failure("unknown method"));
            }
        };
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('EXTERNAL_OFB_PAY')")
    public ResponseEntity<ResOfbPayWebhook> confirm(@RequestBody ReqOfbPayWebhook request) {
        Logger.logInfo(">>> [OFB_PAY CONFIRM] orderId=" + request.getOrderId() + " transactionId=" + request.getTransactionId());

        if (!extOfbPayService.verifyHashKey("confirm", request.getOrderId(), request.getTransactionId(), request.getAmount(), request.getHashKey())) {
            Logger.logWarn(">>> [OFB_PAY CONFIRM] invalid hashKey for orderId=" + request.getOrderId());
            return ResponseEntity.ok(ResOfbPayWebhook.failure("invalid hashKey"));
        }

        Optional<SchoolSubscriptionPayment> paymentOpt = paymentRepo.findByShopTransactionId(request.getOrderId());
        if (paymentOpt.isEmpty()) {
            Logger.logWarn(">>> [OFB_PAY CONFIRM] orderId not found: " + request.getOrderId());
            return ResponseEntity.ok(ResOfbPayWebhook.failure("orderId not found"));
        }

        SchoolSubscriptionPayment payment = paymentOpt.get();
        long expectedAmountInTiyins = payment.getTotalAmount() * 100;
        if (request.getAmount() == null || expectedAmountInTiyins != request.getAmount()) {
            Logger.logWarn(">>> [OFB_PAY CONFIRM] amount mismatch for orderId=" + request.getOrderId()
                    + " expected=" + expectedAmountInTiyins + " got=" + request.getAmount());
            return ResponseEntity.ok(ResOfbPayWebhook.failure("amount mismatch"));
        }

        payment.setOfbTransactionId(request.getTransactionId());
        paymentRepo.save(payment);

        subscriptionService.processSuccessfulPayment(request.getOrderId());

        return ResponseEntity.ok(ResOfbPayWebhook.success());
    }

    @PostMapping("/accept")
    @PreAuthorize("hasAuthority('EXTERNAL_OFB_PAY')")
    public ResponseEntity<ResOfbPayWebhook> accept(@RequestBody ReqOfbPayWebhook request) {
        Logger.logInfo(">>> [OFB_PAY ACCEPT] orderId=" + request.getOrderId() + " status=" + request.getStatus());

        if (!extOfbPayService.verifyHashKey("accept", request.getOrderId(), request.getTransactionId(), request.getAmount(), request.getHashKey())) {
            Logger.logWarn(">>> [OFB_PAY ACCEPT] invalid hashKey for orderId=" + request.getOrderId());
            return ResponseEntity.ok(ResOfbPayWebhook.failure("invalid hashKey"));
        }

        Optional<SchoolSubscriptionPayment> paymentOpt = paymentRepo.findByShopTransactionId(request.getOrderId());
        if (paymentOpt.isEmpty()) {
            Logger.logWarn(">>> [OFB_PAY ACCEPT] orderId not found: " + request.getOrderId());
            return ResponseEntity.ok(ResOfbPayWebhook.failure("orderId not found"));
        }

        if ("success".equalsIgnoreCase(request.getStatus())) {
            subscriptionService.processSuccessfulPayment(request.getOrderId());
        } else {
            SchoolSubscriptionPayment payment = paymentOpt.get();
            payment.setStatus(TransactionStatus.FAILED);
            paymentRepo.save(payment);
        }

        return ResponseEntity.ok(ResOfbPayWebhook.success());
    }

    @PostMapping("/reverse")
    @PreAuthorize("hasAuthority('EXTERNAL_OFB_PAY')")
    public ResponseEntity<ResOfbPayWebhook> reverse(@RequestBody ReqOfbPayWebhook request) {
        Logger.logInfo(">>> [OFB_PAY REVERSE] orderId=" + request.getOrderId());

        if (!extOfbPayService.verifyHashKey("reverse", request.getOrderId(), request.getTransactionId(), request.getAmount(), request.getHashKey())) {
            Logger.logWarn(">>> [OFB_PAY REVERSE] invalid hashKey for orderId=" + request.getOrderId());
            return ResponseEntity.ok(ResOfbPayWebhook.failure("invalid hashKey"));
        }

        Optional<SchoolSubscriptionPayment> paymentOpt = paymentRepo.findByShopTransactionId(request.getOrderId());
        if (paymentOpt.isEmpty()) {
            Logger.logWarn(">>> [OFB_PAY REVERSE] orderId not found: " + request.getOrderId());
            return ResponseEntity.ok(ResOfbPayWebhook.failure("orderId not found"));
        }

        SchoolSubscriptionPayment payment = paymentOpt.get();
        payment.setStatus(TransactionStatus.CANCELED);
        paymentRepo.save(payment);

        return ResponseEntity.ok(ResOfbPayWebhook.success());
    }
}
