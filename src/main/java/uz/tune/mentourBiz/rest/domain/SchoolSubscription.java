package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;

import java.time.Instant;

@Entity
@Table(name = "school_subscriptions_active")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSubscription extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    private Instant expiresAt;

    @Column(nullable = true)
    private boolean aiExerciseEnabled = true;

    @Column(nullable = true)
    private boolean aiWritingEnabled = true;

    @Column(nullable = true)
    private boolean aiSpeakingEnabled = true;

    private Long tokenLimit = 100000L;

    private Long tokensUsed = 0L;

    private boolean autoFreezeEnabled = false;

    private Integer overdueDaysToFreeze = 7;
}
