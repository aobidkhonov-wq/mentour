package uz.tune.mentourBiz.rest.payload.res.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.ExpenseEnums;

import java.time.LocalDate;
import java.util.List;

/** The KPI cards above the expense list: what was spent in the window, broken down by category. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResExpenseSummary {

    private LocalDate fromDate;
    private LocalDate toDate;

    private Long totalAmount;
    private Long totalCount;

    // Salaries are usually the biggest line, so they get their own card rather than being read off
    // the category list.
    private Long teacherSalaryAmount;

    private List<CategoryTotal> categories;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryTotal {
        private ExpenseEnums.ExpenseCategory category;
        private Long amount;
        private Long count;
        // Share of totalAmount, e.g. 42.5.
        private Double percent;
    }
}
