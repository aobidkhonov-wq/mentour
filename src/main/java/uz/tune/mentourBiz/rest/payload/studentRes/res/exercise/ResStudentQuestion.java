package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingScoresDto;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ResStudentQuestion {
    private UUID questionId;
    private UUID exerciseId;
    private ExerciseType type;
    private QuestionContent content;
    private String instruction;
    private Integer coinReward;


    private Map<String, String> preFilledAnswers;

    private ExerciseSubmissionStatus submissionStatus;
    private String studentEssay;
    private String teacherFeedback;

    private String studentAudioUrl;
    private String transcript;
    private SpeakingScoresDto speakingScores;
    private List<String> feedbackBullets;
}