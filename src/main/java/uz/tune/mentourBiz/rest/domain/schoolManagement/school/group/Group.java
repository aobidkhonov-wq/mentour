package uz.tune.mentourBiz.rest.domain.schoolManagement.school.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.Branch;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.enums.GroupStatus;

import java.util.List;
import java.util.UUID;

@Table(name = "groups")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Group extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @Column(name = "referral_code")
    private UUID referralCode;

    @Column(name = "group_status")
    @Enumerated(EnumType.STRING)
    private GroupStatus groupStatus = GroupStatus.ACTIVE;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lesson_days", columnDefinition = "jsonb")
    private List<String> lessonDays;

    @Formula("(SELECT count(e.id) FROM enrollments e WHERE e.group_id = id AND e.status = 'ONGOING')")
    private Long studentCount;
}
