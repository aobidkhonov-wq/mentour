package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.Region;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResVocabQuizWord {
    private UUID wordUuid;
    private UUID setUuid;
    private String translationUz;
    private String translationRu;
    private String translationTjk;
    private String translationKaa;

    private String word;
    private String partOfSpeech;
    private String instruction;
    private String image;
    private String audioUrl;
    private String primaryTranslation;
    private List<TranslationDto> translations;

    public ResVocabQuizWord(VocabularyWord vocabWord, UUID setUuid, Region region) {
        this.wordUuid = vocabWord.getUuid();
        this.setUuid = setUuid;
        this.word = vocabWord.getWord();
        this.partOfSpeech = vocabWord.getPartOfSpeech();
        this.audioUrl = vocabWord.getAudioUrl();
        this.image = vocabWord.getAttachmentUrl();
        this.instruction = "Translate the word.";

        String country = (region != null && region.getCountry() != null)
                ? region.getCountry().toUpperCase() : "UZBEKISTAN";
        String primaryLangCode = (region != null && region.getLang() != null)
                ? region.getLang().name() : "UZB";

        this.translations = new ArrayList<>();

        switch (country) {
            case "UZBEKISTAN":
                this.translations.add(new TranslationDto("UZ", vocabWord.getTranslationUz(), primaryLangCode.equals("UZB")));
                break;
            case "KARAKALPAKISTAN":
                this.translations.add(new TranslationDto("KAA", vocabWord.getTranslationKaa(), primaryLangCode.equals("KAA")));
                this.translations.add(new TranslationDto("UZ", vocabWord.getTranslationUz(), primaryLangCode.equals("UZB")));
                break;
            case "TAJIKISTAN":
                this.translations.add(new TranslationDto("TG", vocabWord.getTranslationTjk(), primaryLangCode.equals("TJK")));
                break;
            case "KYRGYZSTAN":
                this.translations.add(new TranslationDto("KY", vocabWord.getTranslationKrg(), primaryLangCode.equals("KRG")));
                break;
        }

        this.translations.add(new TranslationDto("ENG", vocabWord.getWord(), primaryLangCode.equals("ENG")));
        this.translations.add(new TranslationDto("RU", vocabWord.getTranslationRu(), primaryLangCode.equals("RUS")));


        this.primaryTranslation = translations.stream()
                .filter(TranslationDto::isPrimary)
                .map(TranslationDto::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(vocabWord.getTranslationUz() != null ? vocabWord.getTranslationUz() : "");
    }
}