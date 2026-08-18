package uz.tune.mentourBiz.rest.payload.req.finance;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.ExpenseEnums;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;

import java.time.LocalDate;
import java.util.UUID;

/** Books an expense by hand. Teacher salaries are not entered here — payroll writes those itself. */
@Getter
@Setter
public class ReqSchoolExpense {

    // Defaults to the caller's own school.
    private UUID schoolUuid;

    private ExpenseEnums.ExpenseCategory category;

    // Positive.
    private Long amount;

    private FinanceEnums.PaymentMethod method;

    // Defaults to today.
    private LocalDate expenseDate;

    private String title;

    private String note;
}
