package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;


import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.enums.ExerciseSubType;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.res.ResSpeaking;
import uz.tune.mentourBiz.rest.payload.res.attempt.SchoolLimitAttempts;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResWriting;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ResUnitTaskInfo {
    private UUID id;
    private String title;
    private Integer sortOrder;
    private Long totalQuestions;
    private Integer percentages;
    private String topic;
    private boolean isAdditional;
    private LessonSectionType lessonSectionType;
    private ExerciseSubType exerciseSubType;
    private List<ResWriting> resWriting;
    private List<ResSpeaking> resSpeaking;
    private boolean answeredAll;
    private boolean limitInstalled;
    private int attempts;
    private ManualGradeInfo manualGrade;

    public ResUnitTaskInfo(UUID uuid, String topic, String title, Integer sortOrder, Long totalQuestions, int percentage, LessonSectionType containerType, SchoolLimitAttempts schoolAttemptLimit) {
        this.id = uuid;
        this.title = title;
        this.sortOrder = sortOrder;
        this.topic = topic;
        this.totalQuestions = totalQuestions;
        this.percentages = percentage;
        this.lessonSectionType = containerType;
        this.limitInstalled = schoolAttemptLimit != null && schoolAttemptLimit.isSetAttempt();
        this.attempts = schoolAttemptLimit != null ? schoolAttemptLimit.getAttempts() : 0;
    }

    public ResUnitTaskInfo(ExerciseTask task, UUID uuid, String topic, String title, Integer sortOrder, Long totalQuestions, int percentage, LessonSectionType containerType,SchoolLimitAttempts schoolAttemptLimit) {
        this.id = uuid;
        this.title = title;
        this.sortOrder = sortOrder;
        this.topic = topic;
        this.isAdditional = !task.getUnit().getSchoolBook().isGlobal();
        this.totalQuestions = totalQuestions;
        this.percentages = percentage;
        this.lessonSectionType = containerType;
        if(task.getSubType() != null){
            this.exerciseSubType = task.getSubType();
        }
        this.limitInstalled = schoolAttemptLimit != null && schoolAttemptLimit.isSetAttempt();
        this.attempts = schoolAttemptLimit != null ? schoolAttemptLimit.getAttempts() : 0;
    }

    public ResUnitTaskInfo(ExerciseTask exerciseTask, UUID uuid, String topic, String title, Integer sortOrder,
                           Long totalQuestions, int percentage,
                           LessonSectionType containerType, List<ResWriting> resWriting,
                           SchoolLimitAttempts schoolAttemptLimit) {
        this.id = uuid;
        this.topic = topic;
        this.title = title;

        this.sortOrder = sortOrder;
        this.totalQuestions = totalQuestions;
        this.percentages = percentage;
        if(exerciseTask.getSubType() != null){
            this.exerciseSubType = exerciseTask.getSubType();
        }
        this.lessonSectionType = containerType;
        this.resWriting = resWriting;
        this.limitInstalled = schoolAttemptLimit != null && schoolAttemptLimit.isSetAttempt();
        this.attempts = schoolAttemptLimit != null ? schoolAttemptLimit.getAttempts() : 0;
    }

    public ResUnitTaskInfo(ExerciseTask exerciseTask, UUID uuid, String topic, String title, Integer sortOrder,
                           Long totalQuestions, int percentage,
                           LessonSectionType containerType,
                           List<ResWriting> resWriting,
                           List<ResSpeaking> resSpeaking,
                           SchoolLimitAttempts schoolAttemptLimit) {
        this.id = uuid;
        this.topic = topic;
        this.title = title;
        this.sortOrder = sortOrder;
        this.totalQuestions = totalQuestions;
        this.percentages = percentage;
        this.lessonSectionType = containerType;
        if(exerciseTask.getSubType() != null){
            this.exerciseSubType = exerciseTask.getSubType();
        }
        this.resWriting = resWriting;
        this.resSpeaking = resSpeaking;
        this.limitInstalled = schoolAttemptLimit != null && schoolAttemptLimit.isSetAttempt();
        this.attempts = schoolAttemptLimit != null ? schoolAttemptLimit.getAttempts() : 0;
    }
}
