package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;
import uz.tune.mentourBiz.rest.payload.res.stats.ResTeacherGroupPayroll;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Everything the Paycheck Details panel needs: the header, the Summary tab's earning and deduction
 * lines, and the data behind the Lessons, Bonuses and Adjustments tabs.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResPayslipDetail {

    private UUID uuid;

    private UUID teacherUuid;
    private String teacherName;
    private String teacherInitials;
    // Free-text role under the name, e.g. "Senior English Teacher". Falls back to the plan name.
    private String teacherTitle;

    private String period;              // "2026-06"
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private PayrollEnums.PayslipStatus status;
    private FinanceEnums.PaymentMethod paymentMethod;
    private LocalDate paymentDate;

    private UUID salaryPlanUuid;
    private String salaryPlanName;

    // ---- Summary tab ----
    private List<Line> earnings;
    private List<Line> deductions;
    private Long totalEarnings;
    private Long totalDeductions;
    private Long netPay;

    // Net pay spelled out for the payslip, e.g. "Two million seven hundred sixty thousand sum only".
    private String netPayInWords;

    // ---- Settlement ----
    // Paid out of the teacher's balance so far, and what this month still owes. Advances show up here,
    // never among the deductions: an advance is pay handed over early, not pay the teacher did not earn.
    private Long paidAmount;
    private Long remainingAmount;

    // The teacher's whole balance, this month included — what the school owes them across every open
    // month, since an unpaid remainder carries over rather than being written off.
    private Long teacherBalance;

    // ---- Lessons tab: the per-group breakdown the amounts came from ----
    private List<ResTeacherGroupPayroll> groups;

    // ---- Bonuses / Adjustments tabs ----
    // The History tab is paginated and filterable, so it has its own call:
    // GET /payroll/payslips/{uuid}/history.
    private List<ResPayrollEvent> bonuses;
    private List<ResPayrollEvent> adjustments;

    private String note;
    private Instant generatedAt;
    private String approvedByName;
    private Instant approvedAt;
    private String paidByName;
    private Instant paidAt;

    /** One row on either side of the Summary tab. */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Line {
        private UUID uuid;
        private PayrollEnums.PayslipLineCategory category;
        private String label;
        // Always positive; which side it belongs to is the list it appears in.
        private Long amount;
        // Lessons taught, reports missed, ... Null for a lump sum.
        private Long quantity;
        private String note;
    }
}
