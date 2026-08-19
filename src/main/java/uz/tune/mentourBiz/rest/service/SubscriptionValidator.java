package uz.tune.mentourBiz.rest.service;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubscriptionValidator {
    private final SchoolRepo schoolRepo;
    private final SchoolSubscriptionRepo subscriptionRepo;

    public void validateStudentAddition(School school, int newCount) {
        SchoolSubscription sub = subscriptionRepo.findBySchool_Uuid(school.getUuid()).orElse(null);

        if (sub != null && sub.getExpiresAt() != null && sub.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException(MessageKey.SUBSCRIPTION_EXPIRED.getKey());
        }

        int currentCount = school.getStudentCount();
        int limit = school.getStudentLimit();

        if (limit > 0 && (currentCount + newCount) > limit) {
            throw new ValidationException(MessageKey.STUDENT_LIMIT_REACHED.getKey());
        }
    }
}

//    public boolean isFeatureAllowed(School school, Subscriptions feature) {
//        if (school.getSubscriptionPlan() == null) return false;
//        return school.getSubscriptionPlan().getIncludedFeatures().contains(feature);
//    }
