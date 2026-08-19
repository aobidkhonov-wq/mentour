package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "unit_exam_settings")
@Getter
@Setter
public class UnitExamSettings extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @Column(name = "separate_section")
    private boolean separateSection;

    @Column(name = "no_screenshot")
    private boolean noScreenshot;

    @Column(name = "attempt_limit")
    private int attemptLimit = 1;

    @Column(name = "time_limit")
    private int timeLimit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_time_limits", columnDefinition = "jsonb")
    private Map<LessonSectionType, Integer> sectionTimeLimits;

    @Column(name = "freeze_screen")
    private boolean freezeScreen;

    @Column(name = "freeze_timer")
    private int freezeTimer;
}