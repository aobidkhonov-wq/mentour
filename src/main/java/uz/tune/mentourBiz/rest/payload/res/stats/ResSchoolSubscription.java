package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResSchoolSubscription {

    private Long tokenLimit;
    private Long tokensUsed;
    private Double tokenUsagePct;
    private Boolean aiExplanationEnabled;
    private Boolean aiExerciseEnabled;
    private Boolean aiSpeakingEnabled;
    private Boolean aiWritingEnabled;
    private Instant expiresAt;
    private Integer overdueDaysToFreeze;

}
