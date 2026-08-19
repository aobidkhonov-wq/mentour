package uz.tune.mentourBiz.rest.payload.res.exercise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResVocabWord {
    private UUID uuid;
    private String word;
    private String translation;
    private String definition;
    private String exampleSentence;
    private ResAttachment logo;
    private String audioUrl;

    public ResVocabWord(VocabularyWord vocabularyWord) {
        this.uuid = UUID.randomUUID();
        this.word = vocabularyWord.getWord();
        this.translation = vocabularyWord.getTranslationUz();
    }
}
