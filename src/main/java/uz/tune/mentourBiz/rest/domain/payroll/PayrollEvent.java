package uz.tune.mentourBiz.rest.domain.payroll;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;

import java.time.Instant;
import java.util.UUID;

/**
 * One line in the payroll history: a lesson that earned money, a bonus awarded, a deduction applied or
 * a manual correction. Events are the audit trail behind a payslip — generating a payslip writes the
 * earnings events, and an admin adding a bonus writes one directly.
 *
 * <p>{@code addedBy} is null for anything payroll produced itself, which is what the History screen
 * shows as "System".
 */
@Table(name = "payroll_events")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayrollEvent extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "event_type")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.PayrollEventType eventType;

    // Headline shown in the log, e.g. "Lesson completed" or "Student Retention Bonus".
    @Column(name = "title")
    private String title;

    // Second line under the headline, e.g. "General English B1" or "2 missing reports".
    @Column(name = "subtitle")
    private String subtitle;

    // Signed: positive for earnings and bonuses, negative for deductions. A correction may be either.
    @Column(name = "amount")
    private Long amount = 0L;

    // Which payslip bucket this event rolls into. Lets an admin book a deduction specifically as
    // "Late Penalty" or "Missing Reports" instead of a nameless "other".
    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private PayrollEnums.PayslipLineCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    // Students the event covered, for group-wide events where naming one student makes no sense.
    @Column(name = "student_count")
    private Long studentCount;

    @Column(name = "note", length = 1000)
    private String note;

    // The payslip this event was rolled into, once one has been generated for its period.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id")
    private TeacherPayslip payslip;

    @Column(name = "period_year")
    private Integer periodYear;

    @Column(name = "period_month")
    private Integer periodMonth;

    // Null when payroll generated the event itself rather than an admin entering it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    private User addedBy;

    @Column(name = "occurred_at")
    private Instant occurredAt;
}
