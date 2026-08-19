package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A reusable, named salary scheme a school assigns to many teachers ("Senior Teacher Plan",
 * "Kids Teacher Plan"). It carries the earning structure plus the bonuses and deductions that come with
 * it; the per-teacher link and any personal overrides live on
 * {@link uz.tune.mentourBiz.rest.domain.userManagement.user.TeacherSalaryPlan}.
 *
 * <p>Every amount may be zero — {@code planType} only says how the plan is presented, it does not
 * restrict which fields are filled in.
 */
@Table(name = "salary_plans")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryPlan extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "plan_type")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.SalaryPlanType planType = PayrollEnums.SalaryPlanType.PERCENTAGE;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.SalaryPlanStatus status = PayrollEnums.SalaryPlanStatus.ACTIVE;

    // ---- Earning structure ----

    // Flat monthly base paid once, regardless of how many groups the teacher runs.
    @Column(name = "fixed_monthly_salary")
    private Long fixedMonthlySalary = 0L;

    // Percent (0..100) of the revenue collected by each group the teacher teaches.
    @Column(name = "percent_of_lesson_value")
    private Integer percentOfLessonValue = 0;

    // Flat amount earned per student per month, prorated by the lessons they attended.
    @Column(name = "fixed_amount_per_lesson")
    private Long fixedAmountPerLesson = 0L;

    // Lessons the teacher must conduct in the month before the plan pays anything. 0 = no requirement.
    @Column(name = "minimum_lessons_requirement")
    private Integer minimumLessonsRequirement = 0;

    @Column(name = "calculation_mode")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.CalculationMode calculationMode = PayrollEnums.CalculationMode.PER_COMPLETED_LESSON;

    // False when the plan only applies to the groups picked per teacher rather than all of them.
    @Column(name = "applies_to_all_groups")
    private Boolean appliesToAllGroups = Boolean.TRUE;

    // Income tax withheld from the payslip total, as a percent (0..100).
    @Column(name = "tax_percent")
    private Integer taxPercent = 0;

    @OneToMany(mappedBy = "salaryPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalaryPlanBonus> bonuses = new ArrayList<>();

    @OneToMany(mappedBy = "salaryPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalaryPlanDeduction> deductions = new ArrayList<>();
}
