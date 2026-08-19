package uz.tune.mentourBiz.rest.service.school.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.SchoolPayoutAccount;
import uz.tune.mentourBiz.rest.payload.res.payments.ResPayoutDetails;
import uz.tune.mentourBiz.rest.repository.school.SchoolPayoutAccountRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.school.SchoolPayoutService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolPayoutServiceImpl implements SchoolPayoutService {

    private final SchoolPayoutAccountRepo payoutAccountRepo;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final SchoolRepo schoolRepo;
    private final MessageSingleton messageSingleton;
    private final AuthToViewEntity authToViewEntity;

//    @Override
//    @Transactional
//    public ResponseMessage createOrUpdatePayoutDetails(ReqPayoutDetails request, UUID schoolId) {
//        UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
//        School school = schoolRepo.findByUuid(schoolUuid)
//                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
//
//        SchoolPayoutAccount account = payoutAccountRepo.findBySchool_Uuid(schoolUuid)
//                .orElse(new SchoolPayoutAccount());
//
//        account.setSchool(school);
//        account.setRecipientFullName(request.getRecipientFullName());
//        account.setRecipientAccount(request.getRecipientAccount());
//        account.setRecipientPinfl(request.getRecipientPinfl());
//        account.setBankMfo(request.getBankMfo());
//
//        payoutAccountRepo.save(account);
//
//        return new ResponseMessage("Payout details saved successfully.");
//    }



    @Override
    public ResPayoutDetails getPayoutDetails(UUID schoolId) {
        UUID targetUuid = userScopeService.resolveSchoolUuid(schoolId);
        if (targetUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());

        School school = schoolRepo.findByUuid(targetUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolPayoutAccount account = payoutAccountRepo.findBySchool(school)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.PAYOUT_NOT_CONFIGURED.getKey()));

        return new ResPayoutDetails(account);
    }
}