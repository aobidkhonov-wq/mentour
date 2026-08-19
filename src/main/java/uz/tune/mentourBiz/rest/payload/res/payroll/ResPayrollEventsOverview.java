package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** KPI cards and the event-type legend on the History screen. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResPayrollEventsOverview {

    private Long totalEvents;

    private Long lessonEarnings;
    private Double lessonEarningsPercent;

    private Long bonuses;
    private Double bonusesPercent;

    private Long deductions;
    private Double deductionsPercent;

    private Long adjustments;
    private Double adjustmentsPercent;
}
