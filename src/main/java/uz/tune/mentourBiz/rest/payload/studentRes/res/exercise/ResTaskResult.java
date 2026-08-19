package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.ExerciseType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ResTaskResult {
    private UUID taskId;
    private String title;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer totalCoinsEarned;
    private Integer totalScoreEarned;
    private Integer scorePercentage;
    private List<QuestionResultSummary> questions;
    private boolean aiExplanationEnabled;



    @Getter
    @Setter
    @AllArgsConstructor
    public static class QuestionResultSummary {
        private UUID questionId;
        private ExerciseType type;
        private boolean isCorrect;
        private Integer coinReward;
        private Integer scoreEarned;

        private Object studentAnswer;
        private Object correctAnswer;


        private List<Boolean> orderingFeedback;
        private Map<String, Boolean> gapFeedback;
        private Map<String, Boolean> matchingFeedback;
        private Integer scorePercentage;
        private String audioUrl;
        private String explanation;
    }

    public ResTaskResult(UUID taskId, String title, Integer totalQuestions, Integer correctAnswers,
                         Integer totalCoinsEarned, Integer totalScoreEarned, Integer scorePercentage,
                         List<QuestionResultSummary> questions) {
        this.taskId = taskId;
        this.title = title;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.totalCoinsEarned = totalCoinsEarned;
        this.totalScoreEarned = totalScoreEarned;
        this.scorePercentage = scorePercentage;
        this.questions = questions;
    }
}