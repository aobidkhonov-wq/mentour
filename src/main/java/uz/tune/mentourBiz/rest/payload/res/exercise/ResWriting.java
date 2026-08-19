package uz.tune.mentourBiz.rest.payload.res.exercise;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.writing.WritingSubmission;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.UUID;

@Getter
@Setter
public class ResWriting {
    private ResExerciseQuestion resExerciseQuestion;
    private String studentAnswer;
    private String previousFeedback;
    private Integer score;
    private Integer coinsAwarded;
    private Integer scoreReward;
    private ExerciseSubmissionStatus status;
    private UUID submissionUuid;
    private Integer grammarScore;
    private Integer vocabularyScore;
    private Integer coherenceScore;


    public ResWriting(ResExerciseQuestion resExerciseQuestion, String studentAnswer, String previousFeedback, WritingSubmission writingSubmission) {
        this.resExerciseQuestion = resExerciseQuestion;
        this.studentAnswer = studentAnswer;
        if(CoreUtils.isPresent(previousFeedback)) {
            this.previousFeedback = previousFeedback;
        }
        else{
            this.previousFeedback = "";
        }
        if(CoreUtils.isPresent(writingSubmission)) {
            this.score = writingSubmission.getScore();
            this.coinsAwarded = writingSubmission.getCoinsAwarded();
            this.status = writingSubmission.getStatus();
            this.submissionUuid = writingSubmission.getUuid();
        }

        if (writingSubmission != null) {
            this.grammarScore = writingSubmission.getGrammarScore();
            this.vocabularyScore = writingSubmission.getVocabularyScore();
            this.coherenceScore = writingSubmission.getCoherenceScore();
        }
    }
}
