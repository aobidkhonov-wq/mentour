package uz.tune.mentourBiz.rest.payload.req.payroll;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.List;
import java.util.UUID;

/**
 * Create/update payload for a salary plan. Like the teacher plan setup, an omitted amount is stored as
 * 0 and {@code bonuses}/{@code deductions} replace the existing sets wholesale.
 */
@Getter
@Setter
public class ReqSalaryPlan {

    // SYS_ADMIN must say which school; for a school admin it is forced to their own.
    private UUID schoolUuid;

    private String name;
    private String description;
    private PayrollEnums.SalaryPlanType planType;
    private PayrollEnums.SalaryPlanStatus status;

    private Long fixedMonthlySalary;
    private Integer percentOfLessonValue;
    private Long fixedAmountPerLesson;
    private Integer minimumLessonsRequirement;
    private PayrollEnums.CalculationMode calculationMode;
    private Boolean appliesToAllGroups;
    private Integer taxPercent;

    private List<Component> bonuses;
    private List<Component> deductions;

    @Getter
    @Setter
    public static class Component {
        private String name;
        private String description;
        private Long amount;
        private Boolean automatic;
        // Deductions only; ignored on bonuses.
        private PayrollEnums.PayslipLineCategory category;
    }
}
