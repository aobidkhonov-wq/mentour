package uz.tune.mentourBiz.rest.domain.userManagement.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.StudentParentContact;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "students")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseEntity {


    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "is_payment_active")
    private boolean isPaymentActivated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(name = "coin_balance")
    private Long coins = 0L;

    @Column(name = "life_time_coin_balance")
    private Long lifeTimeCoinBalance = 0L;

    @Column(name = "score_balance")
    private Integer score = 0;

    @Column(name = "key_balance")
    private Long keys = 0L;

    @Column(name = "current_balance")
    private Long currentBalance = 0L;

    @Column(name = "is_atrisk")
    private Boolean isAtRisk = Boolean.FALSE;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();

    @Column(name = "parent_link_code")
    private String parentLinkCode;

    @Column(name = "parent_link_code_expires_at")
    private java.time.Instant parentLinkCodeExpiresAt;

    @OneToMany(mappedBy = "student")
    private List<StudentParentContact> parentContacts;

    @Formula("(SELECT count(*) FROM student_parent_contacts eq WHERE eq.student_id = id)")
    private Long parentCount;


}
