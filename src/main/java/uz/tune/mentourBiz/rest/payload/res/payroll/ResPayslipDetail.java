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

    private List<Line> earnings;
    private List<Line> deductions;
    private Long totalEarnings;
    private Long totalDeductions;
    private Long netPay;

    private String netPayInWords;

    private Long paidAmount;
    private Long remainingAmount;

    private Long teacherBalance;

    private List<ResTeacherGroupPayroll> groups;

    private List<ResPayrollEvent> bonuses;
    private List<ResPayrollEvent> adjustments;

    private String note;
    private Instant generatedAt;
    private String approvedByName;
    private Instant approvedAt;
    private String paidByName;
    private Instant paidAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Line {
        private UUID uuid;
        private PayrollEnums.PayslipLineCategory category;
        private String label;
        private Long amount;
        private Long quantity;
        private String note;
    }
}
