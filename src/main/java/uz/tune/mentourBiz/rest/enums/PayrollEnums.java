package uz.tune.mentourBiz.rest.enums;

/** Enumerations behind the teacher payroll module: salary plans, payslips and the payroll event log. */
public class PayrollEnums {

    /**
     * Life cycle of a teacher's monthly payslip. A payslip is generated as DRAFT, sent for review as
     * PENDING, signed off as APPROVED and finally recorded as PAID. Its figures stop being recomputed
     * once it is approved, so an old month never changes under the school's feet.
     */
    public enum PayslipStatus {
        DRAFT, PENDING, APPROVED, PAID, CANCELLED
    }

    /** What a row in the payroll history log represents. */
    public enum PayrollEventType {
        LESSON_EARNING, BONUS, DEDUCTION, ADJUSTMENT
    }

    /** Which side of the payslip a line sits on. */
    public enum PayslipLineKind {
        EARNING, DEDUCTION
    }

    /** The bucket a payslip line is reported under on the payslip breakdown. */
    public enum PayslipLineCategory {
        // Earnings
        FIXED_SALARY,
        REGULAR_LESSONS,
        SUBSTITUTE_LESSONS,
        TRIAL_LESSONS,
        BONUS,
        OTHER_EARNINGS,
        // Deductions
        TAX,
        LATE_PENALTY,
        MISSING_REPORTS,
        OTHER_DEDUCTION
    }

    /** How a salary plan pays, used to label and group plans in the UI. */
    public enum SalaryPlanType {
        PERCENTAGE,
        FIXED_MONTHLY,
        FIXED_PER_LESSON,
        PERCENTAGE_PLUS_FIXED,
        PERCENTAGE_PLUS_BONUS
    }

    public enum SalaryPlanStatus {
        ACTIVE, ARCHIVED
    }

    /** What the plan counts when paying per lesson. */
    public enum CalculationMode {
        PER_COMPLETED_LESSON,
        PER_SCHEDULED_LESSON,
        PER_ATTENDED_STUDENT
    }
}
