package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResQuestionsTasks {
    private String taskTitle;
    private Long questionCount;
    private String instruction;
    private String taskUuid;
    private List<ResStudentQuestion> questions;

    // Homework attempt info (populated for students)
    private Integer attemptLimit;
    private Integer extraAttempts;
    private Integer attemptsUsed;
    private Integer remainingAttempts;

    public ResQuestionsTasks(String title, Long questionCount, String taskUuid, List<ResStudentQuestion> studentQuestions) {
        this.taskTitle = title;
        this.taskUuid = taskUuid;
        this.questionCount = questionCount;
        this.questions = studentQuestions;
    }
}
