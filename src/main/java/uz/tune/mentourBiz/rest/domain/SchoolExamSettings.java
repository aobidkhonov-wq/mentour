package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.Map;

@Entity
@Table(name = "school_exam_settings")
@Getter
@Setter
public class SchoolExamSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

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