package uz.tune.mentourBiz.rest.domain.userManagement.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.payroll.SalaryPlan;

import java.util.UUID;

/**
 * Salary configuration for a single teacher ("teacher billing plan"). One row per teacher.
 *
 * <p>Three independent components, each of which may be zero — a teacher can be paid by any
 * combination of them:
 * <ul>
 *   <li>{@code fixedSalary} — a flat monthly base, paid once regardless of how many groups they run.</li>
 *   <li>{@code percentPerGroup} — a percent cut of the revenue collected by each group they teach.</li>
 *   <li>{@code fixedPerStudent} — a flat amount per student per month, prorated by lessons.</li>
 * </ul>
 *
 * <p>All three default to 0 and are always stored explicitly: an omitted field in the setup request
 * means "this component pays nothing", not "leave the previous value alone". The percent may be
 * overridden for individual groups via {@link TeacherGroupSalaryConfig}.
 *
 * <p>This entity holds configuration only — the actual money figures are computed at read time from
 * {@code finance_transactions}, lessons and attendance.
 */
@Table(name = "teacher_salary_plans")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherSalaryPlan extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", unique = true)
    private Teacher teacher;

    // The school-wide plan this teacher was put on, when one was used. The amounts below still win —
    // assigning a plan copies its structure here, so a teacher can be nudged off it without touching
    // everyone else on the same plan.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_plan_id")
    private SalaryPlan salaryPlan;

    // Fixed monthly base salary, e.g. 3000000. Paid once per month regardless of group count.
    @Column(name = "fixed_salary")
    private Long fixedSalary = 0L;

    // Percent (0..100) of each group's collected revenue the teacher takes, e.g. 30. Applies to every
    // group they teach; TeacherGroupSalaryConfig overrides it for individual groups.
    // Column keeps its original name so the existing data survives the field rename.
    @Column(name = "revenue_share_percent")
    private Integer percentPerGroup = 0;

    // Fixed amount earned per student per month, e.g. 130000. Prorated by lessons: a student who was
    // marked for only part of the month's lessons yields a proportional cut.
    @Column(name = "fixed_per_student")
    private Long fixedPerStudent = 0L;

    // Inactive plans are configured but pay nothing.
    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;
}
