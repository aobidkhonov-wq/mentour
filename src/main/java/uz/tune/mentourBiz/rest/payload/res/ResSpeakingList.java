package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingSubmission;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Data
public class ResSpeakingList {
    private UUID submissionUuid;
    private String studentName;
    private UUID studentUuid;
    private UUID exerciseQuestionUuid;
    private String unitTitle;
    private String transcript;
    private String audioUrl;

    private Integer overallScore;
    private Integer grammarScore;
    private Integer vocabularyScore;
    private Integer coherenceScore;
    private List<String> feedbackBullets;

    private ExerciseSubmissionStatus status;
    private Instant submittedAt;

    public ResSpeakingList(SpeakingSubmission ss) {
        this.submissionUuid = ss.getUuid();
        this.studentUuid = ss.getStudent().getUuid();
        this.exerciseQuestionUuid = ss.getExerciseQuestion().getUuid();

        if (ss.getStudent() != null && ss.getStudent().getUser() != null) {
            this.studentName = ss.getStudent().getUser().getFirstName() + " " + ss.getStudent().getUser().getLastName();
        }

        if (ss.getExerciseQuestion() != null && !ss.getExerciseQuestion().getExerciseTask().isEmpty()) {
            this.unitTitle = ss.getExerciseQuestion().getExerciseTask().get(0).getUnit().getTitle();
        }

        this.transcript = ss.getTranscript();
        if (ss.getAudioAttachment() != null) {
            this.audioUrl = CoreUtils.getBaseFileUrl() + ss.getAudioAttachment().getName();
        }

        if (ss.getScores() != null) {
            this.overallScore = ss.getScores().getOverallScore();
            this.grammarScore = ss.getScores().getGrammarScore();
            this.vocabularyScore = ss.getScores().getVocabularyScore();
            this.coherenceScore = ss.getScores().getCoherenceScore();
        }

        this.feedbackBullets = ss.getFeedbackBullets();
        this.status = ss.getStatus();
        this.submittedAt = ss.getCreatedAt();
    }
}