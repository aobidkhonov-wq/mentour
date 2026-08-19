package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;

import java.util.UUID;


@Table(name = "lesson_videos")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonVideo extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "formula")
    private String formula;

    @Column(name = "description")
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;
}
