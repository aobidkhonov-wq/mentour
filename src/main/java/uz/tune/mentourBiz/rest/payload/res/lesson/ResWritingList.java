package uz.tune.mentourBiz.rest.payload.res.lesson;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.writing.WritingSubmission;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;

import java.time.Instant;
import java.util.UUID;

@Data
public class ResWritingList {
    private UUID submissionUuid;
    private String studentName;
    private UUID studentUuid;
    private UUID questionUuid;
    private String unitTitle;
    private ExerciseSubmissionStatus status;
    private Instant submittedAt;

    public ResWritingList(WritingSubmission ws) {
        this.submissionUuid = ws.getUuid();
        this.studentUuid = ws.getStudent().getUuid();
        this.questionUuid = ws.getExerciseQuestion().getUuid();
        this.studentName = ws.getStudent().getUser().getFirstName() + " " + ws.getStudent().getUser().getLastName();
        if (ws.getExerciseQuestion() != null && !ws.getExerciseQuestion().getExerciseTask().isEmpty()) {
            this.unitTitle = ws.getExerciseQuestion().getExerciseTask().get(0).getUnit().getTitle();
        }
        this.status = ws.getStatus();
        this.submittedAt = ws.getCreatedAt();
    }
}