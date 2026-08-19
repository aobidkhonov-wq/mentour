package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "unit_exam_sessions")
@Getter
@Setter
public class UnitExamSession extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "start_time")
    private Instant startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_section")
    private LessonSectionType currentSection;

    @Column(name = "section_end_time")
    private Instant sectionEndTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "elapsed_time", columnDefinition = "jsonb")
    private Map<LessonSectionType, Integer> elapsedSeconds = new HashMap<>();

    @Column(name = "is_blocked")
    private boolean isBlocked = false;

    @Column(name = "is_finished")
    private boolean isFinished = false;
}