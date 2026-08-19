package uz.tune.mentourBiz.rest.domain.schoolManagement.school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.Attachment;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.Region;
import uz.tune.mentourBiz.rest.domain.SubscriptionPlan;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.enums.Subscriptions;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table(name = "schools")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class School extends BaseEntity {

    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_id")
    private Attachment logo;

    @Column(name = "is_payment_active")
    private boolean isPaymentActive = false;

    @Column(name = "payment_activated_time")
    private java.time.Instant paymentActivatedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    private User paymentActivatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "contact_info")
    private String contactInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SchoolStatus status;

    @Column(name = "telgeram_link")
    private String telegramLink;

    @Column(name = "utc_offset")
    private Integer utcOffset;



    @ElementCollection(targetClass = Subscriptions.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "school_subscriptions", joinColumns = @JoinColumn(name = "school_id"))
    @Column(name = "subscription", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Subscriptions> activeSubscriptions;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "subscription_plan_id", nullable = true)
    private SubscriptionPlan subscriptionPlan;

    public Integer getStudentLimit() {
        return (subscriptionPlan != null) ? ((subscriptionPlan.getMaxStudents() != null) ? subscriptionPlan.getMaxStudents() : 100) : 100;
    }


    @ManyToMany
    @JoinTable(
            name = "school_allowed_books",
            joinColumns = @JoinColumn(name = "schools_id"),
            inverseJoinColumns = @JoinColumn(name = "school_book_id")
    )
    private List<SchoolBook> allowedBooks;

//    @Column(name = "payment_due_date")
//    private Integer paymentDueDate;

    @Formula("(SELECT count(*) FROM groups gr " +
            "INNER JOIN branches b ON b.id = gr.branch_id " +
            "WHERE b.school_id = id AND gr.group_status != 'DELETED')")
    private Integer classCount;

    @Formula("(SELECT count(DISTINCT s.id) FROM students s " +
            "INNER JOIN users u ON u.id = s.user_id " +
            "INNER JOIN enrollments e ON e.student_id = s.id " +
            "WHERE s.school_id = id " +
            "AND u.status = 'ACTIVE') ")
    private Integer studentCount;

    @Formula("(SELECT count(*) FROM teacher s " +
            "INNER JOIN users u ON u.id = s.user_id " +
            "WHERE s.school_id = id AND u.status != 'BLOCKED')")
    private Integer teacherCount;
}
