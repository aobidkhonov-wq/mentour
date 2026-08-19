package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResSchoolSubscription {
    private String status;
    private ResSubscriptionPlan resSubscriptionPlan;
    private Long daysRemaining;
    private Instant expiresAt;
    private boolean aiExerciseEnabled;
    private boolean aiWritingEnabled;
    private boolean aiSpeakingEnabled;
    private Long tokenLimit;
    private Long tokensUsed;
    private boolean autoFreezeEnabled;
    private Integer overdueDaysToFreeze;
    private boolean isLinkedToOrganization;

    public ResSchoolSubscription(School school, SchoolSubscription sub) {
        Instant now = Instant.now().plusSeconds(3600L *school.getUtcOffset());

        this.resSubscriptionPlan = (school.getSubscriptionPlan() != null)
                ? new ResSubscriptionPlan(school.getSubscriptionPlan())
                : null;

        this.isLinkedToOrganization = school.getOrganization() != null;

        if (sub != null) {
            this.expiresAt = sub.getExpiresAt();
            this.aiExerciseEnabled = sub.isAiExerciseEnabled();
            this.aiWritingEnabled = sub.isAiWritingEnabled();
            this.aiSpeakingEnabled = sub.isAiSpeakingEnabled();
            this.tokenLimit = sub.getTokenLimit();
            this.tokensUsed = sub.getTokensUsed();
            this.autoFreezeEnabled = sub.isAutoFreezeEnabled();
            this.overdueDaysToFreeze = sub.getOverdueDaysToFreeze();

            if (sub.getExpiresAt() != null && sub.getExpiresAt().isAfter(now)) {
                this.status = "ACTIVE";
                this.daysRemaining = java.time.Duration.between(now, sub.getExpiresAt()).toDays();
            } else {
                this.status = "EXPIRED";
                this.daysRemaining = 0L;
            }
        } else {
            this.status = "INACTIVE";
            this.daysRemaining = 0L;
        }
    }
}