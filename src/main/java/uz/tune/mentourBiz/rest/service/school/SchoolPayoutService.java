package uz.tune.mentourBiz.rest.service.school;

import uz.tune.mentourBiz.rest.payload.res.payments.ResPayoutDetails;

import java.util.UUID;

public interface SchoolPayoutService {
//    ResponseMessage createOrUpdatePayoutDetails(ReqPayoutDetails request, UUID schoolId);
    ResPayoutDetails getPayoutDetails(UUID schoolUuid);
}