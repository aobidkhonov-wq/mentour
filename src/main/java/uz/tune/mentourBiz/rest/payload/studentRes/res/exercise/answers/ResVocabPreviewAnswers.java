package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.answers;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.VocabularyAnswer;

import java.util.UUID;

@Getter
@Setter
public class ResVocabPreviewAnswers {
    private UUID vocabQuestionUuid;
    private String wordEn;
    private String attachmentUrl;

    private String studentAnswer;
    private Boolean isCorrect;
    private String errorExplanation;

    public ResVocabPreviewAnswers(VocabularyQuestion q, VocabularyAnswer ans) {
        this.vocabQuestionUuid = q.getUuid();
        this.wordEn = q.getVocabularyWord().getWord();
        this.attachmentUrl = q.getVocabularyWord().getAttachmentUrl();

        if (ans != null) {
            this.studentAnswer = ans.getAnswerContent();
            this.isCorrect = ans.getIsCorrect();
            this.errorExplanation = ans.getErrorExplanation();
        }
    }
}