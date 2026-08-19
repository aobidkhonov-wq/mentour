package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.Region;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResVocabLearnWord {

    private UUID uuid;
    private String word;
    private String translation;
    private String definition;
    private String exampleSentence;
    private String partOfSpeech;
    private String transcription;
    private String audioUrl;
    private String image;

    private Integer coinReward;
    private Integer scoreReward;

    // todo remove
    private String translationUz;
    private String translationRu;
    private String translationTjk;
    private String translationKaa;
    private String translationKrg;


    private String primaryTranslation;

    private List<TranslationDto> translations;

    public ResVocabLearnWord(VocabularyWord v) {
        this(v, null, null);
    }

    public ResVocabLearnWord(VocabularyWord v, VocabularyQuestion q) {
        this(v, q, null);
    }

    public ResVocabLearnWord(VocabularyWord v, VocabularyQuestion q, Region region) {
        this.uuid = v.getUuid();
        this.word = v.getWord();
        this.definition = v.getDefinition();
        this.partOfSpeech = v.getPartOfSpeech();
        this.transcription = v.getTranscription();
        this.exampleSentence = v.getExampleSentence();
        this.audioUrl = v.getAudioUrl();
        this.image = v.getAttachmentUrl();

        this.translationUz = v.getTranslationUz();
        this.translationRu = v.getTranslationRu();
        this.translationTjk = v.getTranslationTjk();
        this.translationKaa = v.getTranslationKaa();
        this.translationKrg = v.getTranslationKrg();

        if (q != null) {
            this.coinReward = q.getCoinReward();
            this.scoreReward = q.getScoreReward();
        }

        String country = (region != null && region.getCountry() != null)
                ? region.getCountry().toUpperCase() : "UZBEKISTAN";
        String primaryLangCode = (region != null && region.getLang() != null)
                ? region.getLang().name() : "UZB";
        
        this.translations = new ArrayList<>();

        switch (country) {
            case "UZBEKISTAN":
                this.translations.add(new TranslationDto("UZ", v.getTranslationUz(), primaryLangCode.equals("UZB")));
                break;
            case "KARAKALPAKISTAN":
                this.translations.add(new TranslationDto("KAA", v.getTranslationKaa(), primaryLangCode.equals("KAA")));
                this.translations.add(new TranslationDto("UZ", v.getTranslationUz(), primaryLangCode.equals("UZB")));
                break;
            case "TAJIKISTAN":
                this.translations.add(new TranslationDto("TG", v.getTranslationTjk(), primaryLangCode.equals("TJK")));
                break;
            case "KYRGYZSTAN":
                this.translations.add(new TranslationDto("KY", v.getTranslationKrg(), primaryLangCode.equals("KRG")));
                break;
        }

        this.translations.add(new TranslationDto("ENG", v.getWord(), primaryLangCode.equals("ENG")));
        this.translations.add(new TranslationDto("RU", v.getTranslationRu(), primaryLangCode.equals("RUS")));



        this.primaryTranslation = translations.stream()
                .filter(TranslationDto::isPrimary)
                .map(TranslationDto::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(v.getTranslationUz() != null ? v.getTranslationUz() : "");

        this.translation = this.primaryTranslation;
    }

    
}