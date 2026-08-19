package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResEnrollmentSummary {

    private Long totalEnrolled;
    private Long newThisMonth;
    private Long totalDeleted;
    private Long deletedThisMonth;

}
