package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResAcademicConfig {
    private Integer minScoreToPass;
    private Integer maxRetries;
    private Integer penaltyPerAttempt;
    private Boolean penaltyEnabled;
    private Integer attendanceThreshold;
    private Integer resultsThreshold;
    private Long teacherMonthlyCoinLimit;
    private Integer defaultCourseLengthDays;
    private Boolean isAttemptInstalled;
    private Boolean defaultCourseLengthInstalled;

    public ResAcademicConfig(SchoolAcademicConfig config) {
        this.minScoreToPass = config.getMinScoreToPass();
        this.maxRetries = config.getMaxRetries();
        this.penaltyPerAttempt = config.getPenaltyPerAttempt();
        this.penaltyEnabled = config.isPenaltyEnabled();
        this.attendanceThreshold = config.getAttendanceThreshold();
        this.resultsThreshold = config.getResultsThreshold();
        this.teacherMonthlyCoinLimit = config.getTeacherMonthlyCoinLimit();
        this.defaultCourseLengthDays = config.getDefaultCourseLengthDays();
        this.isAttemptInstalled = config.isAttemptInstalled();
        this.defaultCourseLengthInstalled=config.isDefaultCourseLengthInstalled();
    }
}
