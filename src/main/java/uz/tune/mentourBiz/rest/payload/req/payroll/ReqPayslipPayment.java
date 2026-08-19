package uz.tune.mentourBiz.rest.payload.req.payroll;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;

import java.time.LocalDate;

/** Records that a payslip has actually been paid out. */
@Getter
@Setter
public class ReqPayslipPayment {

    private FinanceEnums.PaymentMethod paymentMethod;

    // Defaults to today when omitted.
    private LocalDate paymentDate;

    private String note;
}
