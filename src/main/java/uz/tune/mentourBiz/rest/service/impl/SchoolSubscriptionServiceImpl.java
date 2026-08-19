package uz.tune.mentourBiz.rest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.external.ExtSelloService;
import uz.tune.mentourBiz.external.octo.ExtOctoService;
import uz.tune.mentourBiz.external.octo.SchoolSubscriptionPayment;
import uz.tune.mentourBiz.external.ofbpay.ExtOfbPayService;
import uz.tune.mentourBiz.external.payload.res.ExtResOrderCreateResult;
import uz.tune.mentourBiz.external.uzum.ExtUzumService;
import uz.tune.mentourBiz.external.uzum.UzumInvoiceResult;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.SubscriptionPlan;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.PaymentProvider;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.ReqRenewSub;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolSubscriptionSettings;
import uz.tune.mentourBiz.rest.payload.res.ResPaymentInitiation;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscription;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscriptionPayment;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.OrganizationRepository;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionPaymentRepo;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.service.SchoolSubscriptionService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolSubscriptionServiceImpl implements SchoolSubscriptionService {

    private final SchoolSubscriptionRepo repo;
    private final SchoolRepo schoolRepo;
    private final OrganizationRepository organizationRepository;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final AuthToViewEntity authToViewEntity;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final SchoolSubscriptionPaymentRepo schoolSubscriptionPaymentRepo;
    private final ExtSelloService extSelloService;
    private final ExtOctoService extOctoService;
    private final ExtOfbPayService extOfbPayService;
    private final ExtUzumService extUzumService;

    @Override
    @Transactional(readOnly = true)
    public ResSchoolSubscription getSubscription(UUID schoolUuid) {
        User user = userService.getCurrentUser();

        UUID targetUuid = (schoolUuid != null) ? schoolUuid : userScopeService.getCurrentUserSchoolUuid();

        if (targetUuid == null) {
            throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        }

        School school = schoolRepo.findByUuid(targetUuid).orElseThrow();
        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolSubscription sub = repo.findBySchool_Uuid(targetUuid).orElse(null);
        return new ResSchoolSubscription(school, sub);
    }

    @Override
    @Transactional
    public ResPaymentInitiation initiateSubscriptionPayment(Integer months, PaymentProvider provider, UUID organizationUuid) {
        User user = userService.getCurrentUser();
        SchoolSubscriptionPayment payment = new SchoolSubscriptionPayment();
        payment.setMonths(months);
        payment.setShopTransactionId(UUID.randomUUID().toString());
        payment.setStatus(TransactionStatus.PENDING);

        SubscriptionPlan plan;

        if (user.getRole() == UserRole.SCHOOL_DIRECTOR) {
            Organization org = schoolDirectorRepo.findByUser(user)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()))
                    .getOrganization();
            payment.setOrganization(org);
            plan = org.getSubscriptionPlan();
        }
        else if (user.getRole() == UserRole.SYS_ADMIN && organizationUuid != null) {
            Organization org = organizationRepository.findByUuid(organizationUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));
            payment.setOrganization(org);
            plan = org.getSubscriptionPlan();
        }
        else {
            // Regular School Admin or SysAdmin paying for a specific branch
            UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
            School school = schoolRepo.findByUuid(schoolUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
            payment.setSchool(school);
            plan = school.getSubscriptionPlan();
        }

        if (plan == null) {
            throw new ValidationException(MessageKey.SUBSCRIPTION_PLAN_NOT_FOUND.getKey());
        }

        payment.setPlan(plan);
        payment.setTotalAmount(plan.getPrice() * months);
        schoolSubscriptionPaymentRepo.save(payment);

        return getPaymentUrl(payment, provider);
    }

//    @Override
//    @Transactional
//    public String initiateOrganizationSubscriptionPayment(UUID organizationUuid, Integer months, PaymentProvider provider) {
//        User user = userService.getCurrentUser();
//        UUID targetOrgUuid;
//
//        if (user.getRole() == UserRole.SCHOOL_DIRECTOR) {
//            SchoolDirector director = schoolDirectorRepo.findByUser(user)
//                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()));
//            targetOrgUuid = director.getOrganization().getUuid();
//        } else if (user.getRole() == UserRole.SYS_ADMIN) {
//            if (organizationUuid == null) {
//                throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
//            }
//            targetOrgUuid = organizationUuid;
//        } else {
//            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
//        }
//
//        Organization org = organizationRepository.findByUuid(targetOrgUuid)
//                .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));
//
//        SubscriptionPlan plan = org.getSubscriptionPlan();
//        if (plan == null) {
//            throw new ValidationException(MessageKey.SUBSCRIPTION_PLAN_NOT_FOUND.getKey());
//        }
//
//        SchoolSubscriptionPayment payment = new SchoolSubscriptionPayment();
//        payment.setOrganization(org);
//        payment.setPlan(plan);
//        payment.setMonths(months);
//        payment.setTotalAmount(plan.getPrice() * months);
//        payment.setShopTransactionId(UUID.randomUUID().toString());
//        payment.setStatus(TransactionStatus.PENDING);
//        schoolSubscriptionPaymentRepo.save(payment);
//
//        return getPaymentUrl(payment, provider);
//    }

    private ResPaymentInitiation getPaymentUrl(SchoolSubscriptionPayment payment, PaymentProvider provider) {
        if (provider == PaymentProvider.OCTO) {
            Map<String, Object> octoData = extOctoService.preparePayment(payment);
            if (octoData == null) throw new RuntimeException("Octo gateway error");
            payment.setOctoPaymentUuid((String) octoData.get("octo_payment_UUID"));
            schoolSubscriptionPaymentRepo.save(payment);
            return ResPaymentInitiation.ofPayUrl((String) octoData.get("octo_pay_url"));
        }

        if (provider == PaymentProvider.SELLO) {
            ExtResOrderCreateResult selloResult = extSelloService.createSubscriptionInvoice(
                    payment.getShopTransactionId(),
                    payment.getTotalAmount(),
                    payment.getPlan().getCurrency().name()
            );
            if (selloResult == null || CoreUtils.isEmpty(selloResult.getPayLink())) {
                throw new ValidationException(MessageKey.GATEWAY_ERROR.getKey());
            }

            payment.setSelloTransactionId(selloResult.getTransactionId());
            schoolSubscriptionPaymentRepo.save(payment);

            return ResPaymentInitiation.ofPayUrl(selloResult.getPayLink());
        }

        if (provider == PaymentProvider.OFB_PAY) {
            String formAction = extOfbPayService.getPayFormAction();
            if (CoreUtils.isEmpty(formAction)) {
                throw new ValidationException(MessageKey.GATEWAY_ERROR.getKey());
            }

            Map<String, String> formFields = extOfbPayService.buildPaymentFormFields(
                    payment.getShopTransactionId(),
                    payment.getTotalAmount(),
                    payment.getPlan().getName(),
                    payment.getPlan().getCurrency()
            );

            return ResPaymentInitiation.ofForm(formAction, formFields);
        }

        if (provider == PaymentProvider.UZUM) {
            String clientId = payment.getOrganization() != null
                    ? payment.getOrganization().getUuid().toString()
                    : payment.getSchool().getUuid().toString();
            UzumInvoiceResult uzumResult = extUzumService.createSubscriptionInvoice(
                    payment.getShopTransactionId(),
                    clientId,
                    payment.getTotalAmount(),
                    payment.getPlan().getCurrency()
            );
            if (uzumResult == null || CoreUtils.isEmpty(uzumResult.getPayLink())) {
                throw new ValidationException(MessageKey.GATEWAY_ERROR.getKey());
            }

            payment.setUzumTransactionId(uzumResult.getTransactionId());
            schoolSubscriptionPaymentRepo.save(payment);

            return ResPaymentInitiation.ofPayUrl(uzumResult.getPayLink());
        }

        throw new ValidationException(MessageKey.GATEWAY_ERROR.getKey());
    }

    @Override
    @Transactional
    public void processSuccessfulPayment(String shopTransactionId) {
        SchoolSubscriptionPayment payment = schoolSubscriptionPaymentRepo.findByShopTransactionId(shopTransactionId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.PAYMENT_RECORD_NOT_FOUND.getKey()));

        if (payment.getStatus().equals(TransactionStatus.SUCCESS)) return;

        payment.setStatus(TransactionStatus.SUCCESS);
        schoolSubscriptionPaymentRepo.save(payment);

        Organization org = payment.getOrganization();
        Instant now = Instant.now();
        long daysToAdd = 30L * payment.getMonths();

        if (org != null) {
            Instant currentOrgExpiry = org.getExpiresAt();
            Instant orgBase = (currentOrgExpiry != null && currentOrgExpiry.isAfter(now)) ? currentOrgExpiry : now;
            org.setExpiresAt(orgBase.plus(java.time.Duration.ofDays(daysToAdd)));
            organizationRepository.save(org);

            for (School s : org.getSchools()) {
                if (!s.isPaymentActive()) {
                    s.setPaymentActive(true);
                    s.setPaymentActivatedTime(now);
                    schoolRepo.save(s);
                }

                SchoolSubscription sub = repo.findBySchool_Uuid(s.getUuid()).orElse(new SchoolSubscription());
                sub.setSchool(s);
                sub.setExpiresAt(org.getExpiresAt());
                repo.save(sub);
            }
        } else {
            School s = payment.getSchool();
            if (!s.isPaymentActive()) {
                s.setPaymentActive(true);
                s.setPaymentActivatedTime(now);
                schoolRepo.save(s);
            }

            SchoolSubscription sub = repo.findBySchool_Uuid(s.getUuid()).orElse(new SchoolSubscription());
            sub.setSchool(s);
            Instant current = sub.getExpiresAt();
            Instant base = (current != null && current.isAfter(now)) ? current : now;
            sub.setExpiresAt(base.plus(java.time.Duration.ofDays(daysToAdd)));
            repo.save(sub);
        }
    }

    @Override
    @Transactional
    public ResponseMessage updateSubscriptionSettings(UUID schoolUuid, ReqSchoolSubscriptionSettings req) {
        User user = userService.getCurrentUser();
        UUID targetUuid = (user.getRole().equals(UserRole.SYS_ADMIN)) ? schoolUuid : userScopeService.getCurrentUserSchoolUuid();

        SchoolSubscription sub = repo.findBySchool_Uuid(targetUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBSCRIPTION_SETTINGS_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchool(sub.getSchool());

        if (req.getAutoFreezeEnabled() != null) sub.setAutoFreezeEnabled(req.getAutoFreezeEnabled());
        if (req.getOverdueDaysToFreeze() != null) sub.setOverdueDaysToFreeze(req.getOverdueDaysToFreeze());
        if (req.getAiExerciseEnabled() != null) sub.setAiExerciseEnabled(req.getAiExerciseEnabled());
        if (req.getAiWritingEnabled() != null) sub.setAiWritingEnabled(req.getAiWritingEnabled());
        if (req.getAiSpeakingEnabled() != null) sub.setAiSpeakingEnabled(req.getAiSpeakingEnabled());

        repo.save(sub);
        return new ResponseMessage("Subscription settings updated.");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResSchoolSubscription> getExpiringSoon(Integer days, Pageable pageable) {
        Instant threshold = Instant.now().plus(java.time.Duration.ofDays(days));
        return repo.findAllByExpiresAtBeforeAndExpiresAtAfter(threshold, Instant.now(), pageable)
                .map(sub -> new ResSchoolSubscription(sub.getSchool(), sub));
    }

    @Override
    @Transactional
    public ResponseMessage bulkRenew(ReqRenewSub reqRenewSub) {
        for (UUID uuid : reqRenewSub.getSchoolUuids()) {
            SchoolSubscription sub = repo.findBySchool_Uuid(uuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBSCRIPTION_EXPIRED.getKey()));
            sub.setExpiresAt(reqRenewSub.getDate());
            repo.save(sub);
        }
        return new ResponseMessage("Renewed " + reqRenewSub.getSchoolUuids().size() + " schools.");
    }

    @Override
    @Transactional
    public Page<ResSchoolSubscriptionPayment> getSubscriptionPaymentHistory(UUID schoolUuid, TransactionStatus status, Pageable pageable) {
        User user = userService.getCurrentUser();
        Collection<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        Page<SchoolSubscriptionPayment> paymentsPage;

        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
            if (schoolUuid != null) {
                paymentsPage = schoolSubscriptionPaymentRepo.findAllBySchool_Uuid(schoolUuid, pageable);
            } else if (status != null) {
                paymentsPage = schoolSubscriptionPaymentRepo.findAllByStatus(status, pageable);
            } else {
                paymentsPage = schoolSubscriptionPaymentRepo.findAll(pageable);
            }
        } else {
            paymentsPage = schoolSubscriptionPaymentRepo.findAllBySchool_UuidIn(authorizedUuids, pageable);
        }

        paymentsPage.getContent().forEach(payment -> {
            if (TransactionStatus.SUCCESS.equals(payment.getStatus())
                    && payment.getOfdUrl() == null
                    && payment.getSelloTransactionId() != null) {

                try {
                    String freshOfdUrl = extSelloService.getOfdUrl(payment.getSelloTransactionId());

                    if (freshOfdUrl != null && !freshOfdUrl.isBlank()) {
                        payment.setOfdUrl(freshOfdUrl);
                        schoolSubscriptionPaymentRepo.save(payment);
                        Logger.logInfo("Successfully recovered missing OFD URL for Order: " + payment.getShopTransactionId());
                    }
                } catch (Exception e) {
                    Logger.logWarn("Auto-fetch OFD URL failed for Sello ID " + payment.getSelloTransactionId() + ": " + e.getMessage());
                }
            }
        });

        return paymentsPage.map(ResSchoolSubscriptionPayment::new);
    }
}