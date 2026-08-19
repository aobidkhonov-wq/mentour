package uz.tune.mentourBiz.rest.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.payload.res.ResFinanceSummary;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResOrganizationFinanceSummary {
    private Long totalExpectedRevenue;
    private Long collectedRevenue;
    private Long outstandingBalance;
    private Long totalBonusRevenue;
    private Double collectionRate;
    private Double growthRate;
    private List<ResFinanceSummary> schoolSummaries;
}