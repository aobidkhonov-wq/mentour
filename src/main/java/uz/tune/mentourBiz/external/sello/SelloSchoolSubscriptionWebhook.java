package uz.tune.mentourBiz.external.sello;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.rest.payload.req.transaction.ReqTransactionChangeStatus;
import uz.tune.mentourBiz.rest.payload.req.transaction.ReqTransactionCheck;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionPaymentRepo;
import uz.tune.mentourBiz.rest.service.SchoolSubscriptionService;

@RestController
@RequestMapping("/api/v1/public/sello/subscription")
@RequiredArgsConstructor
public class SelloSchoolSubscriptionWebhook {

    private final SchoolSubscriptionPaymentRepo subscriptionPaymentRepo;
    private final SchoolSubscriptionService subscriptionService;

    @PostMapping("/check")
    @PreAuthorize("hasAuthority('EXTERNAL_SELLO')")
    public ResponseEntity<Void> checkInvoice(@RequestBody ReqTransactionCheck request) {
        return subscriptionPaymentRepo.findByShopTransactionId(request.getOrderId())
                .filter(p -> p.getTotalAmount() * 100L == request.getAmount().longValue())
                .map(p -> ResponseEntity.ok().<Void>build())
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('EXTERNAL_SELLO')")
    public ResponseEntity<Void> updateStatus(@RequestBody ReqTransactionChangeStatus request) {
        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            subscriptionService.processSuccessfulPayment(request.getOrderId());
        } else {
            subscriptionPaymentRepo.findByShopTransactionId(request.getOrderId()).ifPresent(p -> {
                p.setStatus(uz.tune.mentourBiz.rest.enums.TransactionStatus.getByName(request.getStatus()));
                subscriptionPaymentRepo.save(p);
            });
        }
        return ResponseEntity.ok().build();
    }
}