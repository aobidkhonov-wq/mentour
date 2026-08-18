package uz.tune.mentourBiz.rest.enums;

/** Enumerations behind the teacher payroll module: salary plans, payslips and the payroll event log. */
public class PayrollEnums {

    /**
     * Life cycle of a teacher's monthly payslip. A payslip is generated as DRAFT, sent for review as
     * PENDING and signed off as APPROVED. Its figures stop being recomputed once it is approved, so an
     * old month never changes under the school's feet.
     *
     * <p>From APPROVED onwards the status is driven by money rather than by a button: approving credits
     * the net pay to the teacher's balance, and every payment made against that balance is allocated
     * back to the oldest open payslip. A payslip becomes PARTIALLY_PAID as soon as part of it has been
     * settled and PAID only when nothing is left outstanding.
     */
    public enum PayslipStatus {
        DRAFT, PENDING, APPROVED, PARTIALLY_PAID, PAID, CANCELLED
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

    /**
     * What moved a teacher's balance. The balance is the running sum of these entries and never resets
     * at a month boundary — an unpaid remainder simply stays on the ledger into the next month.
     */
    public enum BalanceEntryType {
        // A payslip was approved: its net pay becomes money the school owes.
        ACCRUAL,
        // Money handed over, whether as an advance or as the final settlement. Always negative.
        PAYMENT,
        // An approval taken back before any of it was paid, cancelling out its ACCRUAL.
        REVERSAL
    }

    /**
     * How a teacher payment was meant. Both types work the same way — they draw down the balance and
     * settle the oldest open payslip first; the distinction is purely for reporting.
     */
    public enum TeacherPaymentType {
        // Paid before the month's pay run is settled.
        ADVANCE,
        // The regular payout.
        SALARY
    }
}
