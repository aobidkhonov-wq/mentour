package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The KPI cards at the top of the payroll Overview screen, for one month. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResPayrollOverview {

    // "2026-06".
    private String period;

    // Net pay of every payslip in the period.
    private Long totalPayroll;

    // Change against the previous month, e.g. 12.8 for "+12.8% vs last month". Null when the previous
    // month has no payroll to compare against.
    private Double changePercentVsLastMonth;

    // Teachers with a payslip this period.
    private Long totalTeachers;

    // Teachers who are active and could be paid, whether or not a payslip exists yet.
    private Long activeTeachers;

    private Long paidAmount;
    private Double paidPercent;

    private Long pendingApprovalAmount;
    private Double pendingApprovalPercent;

    // Teachers already marked paid this period.
    private Long paidTeacherCount;

    // Teachers with no payslip yet — what "Generate Paychecks" would create.
    private Long missingPayslipCount;
}
