package uz.tune.mentourBiz.rest.domain.schoolManagement.school.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;


import java.time.Instant;
import java.util.UUID;


@Table(name = "enrollments")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Enrollment extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    // --- Coin billing (set when the student is charged coins to join the group) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_plan_id")
    private BillingPlan billingPlan;

    @Column(name = "billing_type")
    @Enumerated(EnumType.STRING)
    private BillingPlanType billingType;

    // LESSON_PACK: total lessons bought. Remaining = lessonsTotal - non-excused consumption rows.
    @Column(name = "lessons_total")
    private Integer lessonsTotal;

    // MONTHLY: access is valid until this instant; extended each renewal.
    @Column(name = "paid_until")
    private Instant paidUntil;

    @Column(name = "last_paid_at")
    private Instant lastPaidAt;

    // Total som charged so far for this enrollment (audit).
    @Column(name = "amount_paid")
    private Long amountPaid = 0L;

}
