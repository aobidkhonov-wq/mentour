package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A salary plan as the Plans screen shows it. The list uses the header fields and {@code teacherCount}
 * / {@code monthlyImpact}; opening a plan uses the rest.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResSalaryPlan {

    private UUID uuid;
    private String name;
    private String description;
    private PayrollEnums.SalaryPlanType planType;
    private PayrollEnums.SalaryPlanStatus status;

    private UUID schoolUuid;

    // Teachers currently assigned to this plan.
    private Long teacherCount;

    // What those teachers cost per month, taken from their latest payslips.
    private Long monthlyImpact;

    // ---- Earning structure ----
    private Long fixedMonthlySalary;
    private Integer percentOfLessonValue;
    private Long fixedAmountPerLesson;
    private Integer minimumLessonsRequirement;
    private PayrollEnums.CalculationMode calculationMode;
    private Boolean appliesToAllGroups;
    private Integer taxPercent;

    private List<Component> bonuses;
    private List<Component> deductions;

    private Instant createdAt;
    private Instant updatedAt;

    /** A bonus or deduction attached to the plan. */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Component {
        private UUID uuid;
        private String name;
        private String description;
        private Long amount;
        // True when payroll awards or withholds it on its own rather than waiting for an admin.
        private Boolean automatic;
        // Deductions only: which bucket the amount lands in on the payslip.
        private PayrollEnums.PayslipLineCategory category;
    }
}
