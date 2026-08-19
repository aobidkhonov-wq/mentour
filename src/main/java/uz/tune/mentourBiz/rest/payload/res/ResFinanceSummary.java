package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResFinanceSummary {
    private UUID schoolUuid;
    private String schoolName;
    private Long totalExpectedRevenue;
    private Long collectedRevenue;
    private Long outstandingBalance;
    private Long totalBonusRevenue;
    private Double collectionRate;
    private Double growthRate;
}