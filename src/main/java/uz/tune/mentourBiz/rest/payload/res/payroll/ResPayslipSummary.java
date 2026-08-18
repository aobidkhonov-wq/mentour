package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.LocalDate;
import java.util.UUID;

/** One row of the Teachers Payroll List. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResPayslipSummary {

    private UUID uuid;
    private UUID teacherUuid;
    private String teacherName;

    // Initials for the avatar chip, e.g. "AT".
    private String teacherInitials;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    // Gross pay before deductions — the "Total Pay" column.
    private Long totalPay;
    private Long netPay;

    // How much of netPay has actually been handed over, and what is left. Both are 0 until the
    // payslip is approved: approving is what puts the money on the teacher's balance to be drawn from.
    private Long paidAmount;
    private Long remainingAmount;

    private PayrollEnums.PayslipStatus status;
}
