package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResSchoolGrowthSummary {

    private Long totalRegistered;
    private Long totalActive;
    private Long totalInactive;
    private Long newThisMonth;
    private Double churnRate;

}
