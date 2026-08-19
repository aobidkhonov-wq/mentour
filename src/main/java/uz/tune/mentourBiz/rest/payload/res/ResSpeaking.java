package uz.tune.mentourBiz.rest.payload.res;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingScoresDto;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingSubmission;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ResSpeaking {
    private ResExerciseQuestion resExerciseQuestion;
    private String transcript;
    private String studentAudioUrl;
    private SpeakingScoresDto scores;
    private List<String> feedbackBullets;
    private ExerciseSubmissionStatus status;
    private UUID submissionUuid;
    private Integer attempts;
    private Integer coinReward;
    private Integer scoreReward;

    private ResPronunciationAi aiResponse;

    public ResSpeaking(ResExerciseQuestion resExerciseQuestion,
                       SpeakingSubmission sub,
                       String aiJson,
                       ObjectMapper mapper) {

        this.resExerciseQuestion = resExerciseQuestion;
        this.coinReward = resExerciseQuestion.getCoinReward();
        this.scoreReward = resExerciseQuestion.getScoreReward();

        if (aiJson != null) {
            try {
                this.aiResponse = mapper.readValue(aiJson, ResPronunciationAi.class);
            } catch (Exception e) {
                this.aiResponse = null;
            }
        }

        if (sub != null) {
            this.submissionUuid = sub.getUuid();
            this.transcript = sub.getTranscript();
            this.status = sub.getStatus();
            this.scores = sub.getScores();
            this.feedbackBullets = sub.getFeedbackBullets();
            if (sub.getAudioAttachment() != null) {
                this.studentAudioUrl =
                        CoreUtils.getBaseFileUrl()
                                + sub.getAudioAttachment().getName();
            }
            else{
                this.studentAudioUrl = "";
            }
            if(sub.getAttemptCount() != null) {
                this.attempts = sub.getAttemptCount();
            }
            else {
                this.attempts = 0;
            }

        } else {
            this.status = ExerciseSubmissionStatus.NOT_STARTED;
        }
    }
}