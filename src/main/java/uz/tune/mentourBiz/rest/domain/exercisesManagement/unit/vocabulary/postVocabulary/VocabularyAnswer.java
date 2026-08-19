package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.AiStatus;

import java.util.UUID;

@Table(name = "vocabulary_answers")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyAnswer extends BaseEntity {
    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "answer_content")
    private String answerContent;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_word", nullable = true)
    private VocabularyQuestion vocabularyQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="vocabulary_set")
    private VocabularySet vocabularySet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "score")
    private Integer score = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status")
    private AiStatus aiStatus = AiStatus.NONE;


    @Column(name = "error_explanation", columnDefinition = "TEXT")
    private String errorExplanation;

    @Column(name = "is_manually")
    private Boolean isManually = false;

    @Column(name = "updated_by")
    private String updatedBy;
}
