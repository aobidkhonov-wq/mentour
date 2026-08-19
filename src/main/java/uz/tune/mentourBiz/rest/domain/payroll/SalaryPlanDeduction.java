package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.UUID;

/**
 * A deduction that comes with a salary plan, e.g. "Missing Reports Deduction — 50 000 per missing
 * report". {@code category} decides which bucket it lands in on the payslip breakdown.
 */
@Table(name = "salary_plan_deductions")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryPlanDeduction extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_plan_id")
    private SalaryPlan salaryPlan;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    // Amount withheld each time the deduction applies (per missing report, per late arrival, ...).
    @Column(name = "amount")
    private Long amount = 0L;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.PayslipLineCategory category = PayrollEnums.PayslipLineCategory.OTHER_DEDUCTION;

    @Column(name = "is_automatic")
    private Boolean automatic = Boolean.FALSE;
}
