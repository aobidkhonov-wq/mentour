package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.enums.ExerciseSubType;
import uz.tune.mentourBiz.rest.enums.ExerciseTaskStatus;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.UUID;

@Table(name = "exercise_tasks")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseTask extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "title")
    private String title;

    @Column(name = "topic")
    private String topic;

    @Formula("(SELECT count(*) FROM exercise_questions eq WHERE eq.exercise_task_id = id)")
    private Long questionCount;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExerciseTaskStatus status = ExerciseTaskStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type")
    private LessonSectionType sectionType = LessonSectionType.GRAMMAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_sub_type")
    private ExerciseSubType subType;

}
