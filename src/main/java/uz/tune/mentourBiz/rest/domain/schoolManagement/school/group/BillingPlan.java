package uz.tune.mentourBiz.rest.domain.schoolManagement.school.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;

import java.util.UUID;

@Table(name = "billing_plans")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingPlan extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    // Every billing plan belongs to exactly one school. Plans are never shared across schools.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private BillingPlanType type;

    // LESSON_PACK: price charged per attended lesson. MONTHLY: price charged per paid period.
    @Column(name = "price")
    private Long price;

    // MONTHLY only (optional): lesson cap for one paid period (e.g. 12 lessons / month).
    // Null means no cap — access is purely time-based. Not used by LESSON_PACK.
    @Column(name = "lesson_count")
    private Integer lessonCount;

    // MONTHLY: how many months one paid period lasts (e.g. 1). The period runs from the enrollment's
    // start date to the same day-of-month N months later. Not used by LESSON_PACK.
    @Column(name = "count_month")
    private Integer countMonth;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;
}
