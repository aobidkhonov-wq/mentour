package uz.tune.mentourBiz.rest.domain.userManagement.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;

import java.util.UUID;

/**
 * Optional per-group override of the teacher's default {@code percentPerGroup}. Present only for
 * groups the teacher takes a different cut from (e.g. 40% on one group, the default 30% on the rest).
 * When no config row exists for a group, the plan's default percent applies.
 */
@Table(
        name = "teacher_group_salary_configs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"salary_plan_id", "group_id"})
)
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherGroupSalaryConfig extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_plan_id")
    private TeacherSalaryPlan salaryPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    // Revenue share percent (0..100) for this specific group, overriding the plan default.
    @Column(name = "override_percent")
    private Integer overridePercent;
}
