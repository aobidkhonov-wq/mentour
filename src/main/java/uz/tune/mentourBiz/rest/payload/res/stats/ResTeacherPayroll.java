package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * A teacher's computed salary for one month: the fixed base plus the sum of every active group's
 * revenue-share cut, with a per-group breakdown.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResTeacherPayroll {
    private UUID teacherUuid;
    private String teacherName;

    // Period the payroll was computed for, as "YYYY-MM".
    private String period;

    // Configured fixed monthly base (paid once).
    private Long fixedSalary;

    // Default revenue share percent from the salary plan.
    private Integer defaultPercent;

    private List<ResTeacherGroupPayroll> groups;

    // Totals across all groups, group-wide (every teacher of those groups combined).
    private Long totalBilledRevenue;
    private Long totalCollectedRevenue;

    // The part of totalBilledRevenue that belongs to this teacher, after the lesson-share split.
    // This is the figure the revenue-share salary is calculated from.
    private Long totalTeacherBilledRevenue;

    // The same split applied to totalCollectedRevenue: how much of what this teacher earned has
    // actually been paid in. Reporting only — it does not drive the salary.
    private Long totalTeacherCollectedRevenue;

    // Sum of every group's groupSalary (the variable part).
    private Long totalGroupSalary;

    // fixedSalary + totalGroupSalary — the final monthly payout.
    private Long totalSalary;

    // False when the teacher has no salary plan yet: the figures are revenue-only and salary is null.
    private Boolean salaryPlanConfigured;

    // False when a plan exists but is switched off — every salary figure is then 0, not null.
    private Boolean salaryPlanActive;
}
