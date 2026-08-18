package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** One payment handed to a teacher, and the months it settled. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResTeacherPayment {

    private UUID uuid;
    private UUID teacherUuid;
    private String teacherName;

    private PayrollEnums.TeacherPaymentType type;
    private Long amount;
    private FinanceEnums.PaymentMethod method;
    private LocalDate paymentDate;
    private String note;

    // What the teacher was still owed after this payment went out.
    private Long balanceAfter;

    // Which months the amount was applied to, oldest first.
    private List<Allocation> allocations;

    private String createdByName;
    private Instant createdAt;

    /** How much of the payment went to one month. */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Allocation {
        private UUID payslipUuid;
        private String period;          // "2026-08"
        private Long amount;
        // Where that month stands now the allocation has been applied.
        private PayrollEnums.PayslipStatus payslipStatus;
    }
}
