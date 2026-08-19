package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;

@Entity
@Table(name = "school_academic_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolAcademicConfig extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    private Integer minScoreToPass = 70;

    private Integer maxRetries = 500;
    @Column(name = "attempt_installed", nullable = false, columnDefinition = "boolean default false")
    private boolean isAttemptInstalled = false;

    @Column(name = "default_course_length_installed", nullable = false, columnDefinition = "boolean default false")
    private boolean defaultCourseLengthInstalled = false;

    private Integer penaltyPerAttempt = 5;

    private boolean penaltyEnabled = false;

    private Integer attendanceThreshold = 75;

    private Integer resultsThreshold = 75;

    private Long teacherMonthlyCoinLimit = 5000L;

    private Integer defaultCourseLengthDays = 90;

    private Integer homeworkAttemptLimit = 5;
}
