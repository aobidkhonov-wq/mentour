package uz.tune.mentourBiz.rest.payload.req.payroll;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.UUID;

/**
 * An admin adding a bonus, a deduction or a correction by hand. The amount is signed the way it should
 * hit the payslip, except for DEDUCTION where a positive number is accepted and stored as negative,
 * since "deduct 50 000" is how people say it.
 */
@Getter
@Setter
public class ReqPayrollEvent {

    private UUID teacherUuid;

    // BONUS, DEDUCTION or ADJUSTMENT. LESSON_EARNING is produced by payroll itself and is rejected.
    private PayrollEnums.PayrollEventType eventType;

    private String title;
    private String subtitle;
    private Long amount;

    // Which payslip bucket it belongs to, e.g. LATE_PENALTY or MISSING_REPORTS for a deduction.
    // Defaults to BONUS / OTHER_DEDUCTION / OTHER_EARNINGS depending on the event type.
    private PayrollEnums.PayslipLineCategory category;

    // Which period the event is counted in. Defaults to the current month.
    private Integer year;
    private Integer month;

    // Optional context shown in the "Related to" column.
    private UUID groupUuid;
    private UUID studentUuid;
    private String note;
}
