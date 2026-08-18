package uz.tune.mentourBiz.rest.payload.req.payroll;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.LocalDate;

/**
 * Hands money to a teacher out of their balance.
 *
 * <p>No payslip is named: the amount comes off the running balance and is applied to the oldest month
 * still owing. It cannot exceed the balance, so a month has to be approved before it can be drawn on.
 */
@Getter
@Setter
public class ReqTeacherPayment {

    // ADVANCE or SALARY. Defaults to ADVANCE; the two behave identically and differ only in reporting.
    private PayrollEnums.TeacherPaymentType type;

    // Positive, and at most the teacher's current balance.
    private Long amount;

    // Set this instead of amount to pay off everything outstanding.
    private Boolean payFullBalance;

    private FinanceEnums.PaymentMethod method;

    // Defaults to today.
    private LocalDate paymentDate;

    private String note;
}
