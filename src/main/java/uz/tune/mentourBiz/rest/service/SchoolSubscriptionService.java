package uz.tune.mentourBiz.rest.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.PaymentProvider;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;
import uz.tune.mentourBiz.rest.payload.req.ReqRenewSub;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolSubscriptionSettings;
import uz.tune.mentourBiz.rest.payload.res.ResPaymentInitiation;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscription;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscriptionPayment;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.UUID;

public interface SchoolSubscriptionService {
    ResSchoolSubscription getSubscription(UUID schoolUuid);


    ResponseMessage bulkRenew(ReqRenewSub reqRenewSub);

    Page<ResSchoolSubscription> getExpiringSoon(Integer days, Pageable pageable);

    ResponseMessage updateSubscriptionSettings(UUID schoolUuid, ReqSchoolSubscriptionSettings req);

    ResPaymentInitiation initiateSubscriptionPayment(Integer months, PaymentProvider provider, UUID organizationUuid);

//    String initiateOrganizationSubscriptionPayment(UUID organizatiaonUuid, Integer months, PaymentProvider provider);

    void processSuccessfulPayment(String shopTransactionId);

    Page<ResSchoolSubscriptionPayment> getSubscriptionPaymentHistory(UUID schoolUuid, TransactionStatus status, Pageable pageable);
}