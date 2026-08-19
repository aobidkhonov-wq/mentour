package uz.tune.mentourBiz.rest.payload.res.exercise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.enums.VocabularySetStatus;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResVocabSet {
    private UUID uuid;
    private Integer sortOrder;
    private String title;
    private Integer questionCount;
    private VocabularySetStatus status;
    private Integer percentage;
    private boolean aiExplanationEnabled;
    private boolean answeredAll;

    public ResVocabSet(VocabularySet vocabularySet, Integer percentage) {
        this.uuid = vocabularySet.getUuid();
        this.sortOrder = vocabularySet.getSortOrder();
        this.title = vocabularySet.getTitle();
        this.questionCount = vocabularySet.getQuestionCount();
        this.status = vocabularySet.getStatus();
        this.percentage = percentage;
    }

    public ResVocabSet(VocabularySet vocabularySet, Integer percentage, boolean aiEnabled) {
        this(vocabularySet, percentage);
        this.aiExplanationEnabled = aiEnabled;
    }

    public ResVocabSet(VocabularySet vocabularySet) {
        this.uuid = vocabularySet.getUuid();
        this.sortOrder = vocabularySet.getSortOrder();
        this.title = vocabularySet.getTitle();
        this.questionCount = vocabularySet.getQuestionCount();
        this.status = vocabularySet.getStatus();
    }
}