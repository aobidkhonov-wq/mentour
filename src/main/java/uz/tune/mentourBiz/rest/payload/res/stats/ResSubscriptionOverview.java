package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResSubscriptionOverview {

    private Long totalSchools;
    private Long aiExplanationCount;
    private Long aiExerciseCount;
    private Long aiSpeakingCount;
    private Long aiWritingCount;
    private Long totalTokensUsed;
    private Long totalTokenLimit;
    private Double tokenUsagePct;

}
