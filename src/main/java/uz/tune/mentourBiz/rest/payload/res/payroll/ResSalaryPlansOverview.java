package uz.tune.mentourBiz.rest.payload.res.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The four KPI cards above the salary plan list. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResSalaryPlansOverview {

    // Plans that are not archived.
    private Long totalPlans;

    // Teachers assigned to any of them.
    private Long assignedTeachers;

    // What those assignments add up to per month.
    private Long monthlyPayrollImpact;

    // monthlyPayrollImpact / assignedTeachers, rounded. Zero when nobody is assigned.
    private Long averageMonthlyCostPerTeacher;
}
