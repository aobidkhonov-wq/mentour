package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What a school still owes one teacher, and what it is made of.
 *
 * <p>{@code balance} runs continuously rather than resetting each month: an August payslip approved
 * while July is still half paid shows both months as open, and the balance is their sum.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResTeacherBalance {

    private UUID teacherUuid;
    private String teacherName;
    private String teacherInitials;
    private UUID schoolUuid;

    // Everything approved minus everything paid. This is the ceiling on the next payment.
    private Long balance;

    // Lifetime figures behind the balance, for the two cards above the history.
    private Long totalAccrued;
    private Long totalPaid;

    // Months still owing, oldest first — the order the next payment will be applied in.
    private List<OpenPeriod> openPeriods;

    private LocalDate lastPaymentDate;
    private Long lastPaymentAmount;

    /** One month with something still outstanding. */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenPeriod {
        private UUID payslipUuid;
        private String period;          // "2026-08"
        private Long netPay;
        private Long paidAmount;
        private Long remainingAmount;
        private PayrollEnums.PayslipStatus status;
    }
}
