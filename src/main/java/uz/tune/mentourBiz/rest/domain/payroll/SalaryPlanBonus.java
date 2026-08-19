package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.UUID;

/**
 * A bonus that comes with a salary plan, e.g. "Student Retention Bonus — 200 000 when a student
 * continues next month". {@code automatic} marks the ones payroll is expected to award on its own;
 * the rest are added by an admin as a payroll event when they decide the criteria were met.
 */
@Table(name = "salary_plan_bonuses")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryPlanBonus extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_plan_id")
    private SalaryPlan salaryPlan;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "amount")
    private Long amount = 0L;

    @Column(name = "is_automatic")
    private Boolean automatic = Boolean.FALSE;
}
