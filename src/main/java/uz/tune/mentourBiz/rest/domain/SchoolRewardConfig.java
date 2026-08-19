package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.*;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;

@Entity
@Table(name = "school_reward_configs")
@Getter
@Setter
@NoArgsConstructor

@AllArgsConstructor
public class SchoolRewardConfig extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    @Column(name = "exercise_auto_enabled")
    private boolean exerciseAutoEnabled = false;

    private int gapFillBase = 3;
    private int orderingBase = 1;
    private int matchingBase = 2;
    private int selectionBase = 5;
    private int multiSelectBase = 3;
    private int circleBase = 3;
    private int tracingBase = 2;

    @Column(name = "audio_multiplier_enabled")
    private boolean audioMultiplierEnabled = true;
    private double audioMultiplier = 1.5;

    @Column(name = "vocab_auto_enabled")
    private boolean vocabAutoEnabled = false;
    private int vocabRewardPerWord = 2;
}