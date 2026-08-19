package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.UnitProgressStatus;

import java.util.UUID;

@Table(name = "student_unit_progress")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnitProgress extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "is_video_watched")
    private Boolean isVideoWatched = Boolean.FALSE;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private UnitProgressStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "is_manually_unlocked")
    private boolean isManuallyUnlocked = Boolean.FALSE;

}
