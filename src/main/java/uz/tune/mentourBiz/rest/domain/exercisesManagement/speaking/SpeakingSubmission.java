package uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.Attachment;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.payload.res.ResPronunciationAi;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "speaking_submissions")
public class SpeakingSubmission extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private ExerciseQuestion exerciseQuestion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_attachment_id")
    private Attachment audioAttachment;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scores", columnDefinition = "jsonb")
    private SpeakingScoresDto scores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feedback_bullets", columnDefinition = "jsonb")
    private List<String> feedbackBullets;

    @Enumerated(EnumType.STRING)
    private ExerciseSubmissionStatus status = ExerciseSubmissionStatus.NOT_STARTED;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pronunciation_data", columnDefinition = "jsonb")
    private ResPronunciationAi pronunciationData;

}