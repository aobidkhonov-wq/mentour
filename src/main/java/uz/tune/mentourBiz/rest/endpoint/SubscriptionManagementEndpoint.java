package uz.tune.mentourBiz.rest.endpoint;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.SubscriptionPlan;
import uz.tune.mentourBiz.rest.enums.PaymentProvider;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;
import uz.tune.mentourBiz.rest.payload.req.ReqRenewSub;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolSubscriptionSettings;
import uz.tune.mentourBiz.rest.payload.res.ResPaymentInitiation;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscription;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscriptionPayment;
import uz.tune.mentourBiz.rest.payload.res.ResSubscriptionPlan;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.SubscriptionPlanRepo;
import uz.tune.mentourBiz.rest.service.SchoolSubscriptionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/subscription")
@RequiredArgsConstructor
public class SubscriptionManagementEndpoint {

    private final SchoolSubscriptionService service;
    private final SubscriptionPlanRepo subscriptionPlanRepo;

    @GetMapping("/one")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SUBSCRIPTION_GET)")
    public ResponseEntity<ResSchoolSubscription> getSubscription(@RequestParam(required = false) UUID schoolUuid) {
        return ResponseEntity.ok(service.getSubscription(schoolUuid));
    }

    @GetMapping("/expiring-soon")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).EXPIRING_SOON_GET)")
    public ResponseEntity<Page<ResSchoolSubscription>> getExpiringSoon(
            @RequestParam(defaultValue = "7") Integer days,
            Pageable pageable) {
        return ResponseEntity.ok(service.getExpiringSoon(days, pageable));
    }

    @PatchMapping("/settings")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SETTINGS_UPDATE)")
    public ResponseEntity<ResponseMessage> updateSettings(
            @RequestParam UUID schoolUuid,
            @RequestBody ReqSchoolSubscriptionSettings req) {
        return ResponseEntity.ok(service.updateSubscriptionSettings(schoolUuid, req));
    }

    @PostMapping("/renew")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BULK_RENEW)")
    public ResponseEntity<ResponseMessage> bulkRenew(
            @RequestBody ReqRenewSub reqRenewSub) {
        return ResponseEntity.ok(service.bulkRenew(reqRenewSub));
    }


    @GetMapping(BaseURI.ALL)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ALL_PLANS_GET)")
    public ResponseEntity<List<ResSubscriptionPlan>> getAllPlans() {
        return ResponseEntity.ok(subscriptionPlanRepo.findAll().stream().map(ResSubscriptionPlan::new).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PLAN_CREATE)")
    public ResponseEntity<ResponseMessage> createPlan(@RequestBody ResSubscriptionPlan plan) {
        SubscriptionPlan subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setUuid(UUID.randomUUID());
        subscriptionPlan.setMaxStudents(plan.getMaxStudents());
        subscriptionPlan.setName(plan.getPlanName());
        subscriptionPlan.setCurrency(plan.getCurrency());
        subscriptionPlan.setPrice(plan.getPrice());
        subscriptionPlan.setActive(Boolean.TRUE);
        subscriptionPlanRepo.save(subscriptionPlan);
        return ResponseEntity.ok(new ResponseMessage("created successfully."));
    }

    @PatchMapping("/edit")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).EDIT_PLAN)")
    public ResponseEntity<ResponseMessage> editPlan(@RequestBody ResSubscriptionPlan plan) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepo.findByUuid(plan.getUuid()).orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBSCRIPTION_PLAN_NOT_FOUND.getKey()));
        if (plan.getMaxStudents() != null) subscriptionPlan.setMaxStudents(plan.getMaxStudents());
        if (plan.getPlanName() != null) subscriptionPlan.setName(plan.getPlanName());
        if (plan.getPrice() != null) subscriptionPlan.setPrice(plan.getPrice());
        if (plan.getCurrency() != null) subscriptionPlan.setCurrency(plan.getCurrency());
        subscriptionPlanRepo.save(subscriptionPlan);
        return ResponseEntity.ok(new ResponseMessage("updated successfully."));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PLAN_DELETE)")
    public ResponseEntity<ResponseMessage> deletePlan(@RequestParam UUID uuid) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBSCRIPTION_PLAN_NOT_FOUND.getKey()));
        subscriptionPlanRepo.delete(subscriptionPlan);
        return ResponseEntity.ok(new ResponseMessage("deleted successfully."));
    }

    @PostMapping("/pay")
    @PreAuthorize("hasAnyAuthority('PAY_SUBSCRIPTION', 'PAY_ORG_SUBSCRIPTION')")
    public ResponseEntity<ResPaymentInitiation> paySubscription(
            @RequestParam Integer months,
            @RequestParam PaymentProvider provider,
            @RequestParam(required = false) UUID organizationUuid) {
        return ResponseEntity.ok(service.initiateSubscriptionPayment(months, provider, organizationUuid));
    }

//    @PostMapping("/pay-org")
//    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PAY_ORG_SUBSCRIPTION)")
//    public ResponseEntity<Map<String, String>> payOrgSubscription(
//            @RequestParam(required = false) UUID organizationUuid,
//            @RequestParam Integer months,
//            @RequestParam PaymentProvider provider) {
//        String payUrl = service.initiateOrganizationSubscriptionPayment(organizationUuid, months, provider);
//        return ResponseEntity.ok(java.util.Collections.singletonMap("payUrl", payUrl));
//    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PAYMENT_HISTORY_GET)")
    public ResponseEntity<Page<ResSchoolSubscriptionPayment>> getPaymentHistory(
            @RequestParam(required = false) UUID schoolUuid,
            @RequestParam(required = false) TransactionStatus status,
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.getSubscriptionPaymentHistory(schoolUuid, status, pageable));
    }
}