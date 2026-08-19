package uz.tune.mentourBiz.rest.payload;

import lombok.Data;

import java.util.UUID;


@Data
public class ReqUpsertVocabWord {
    private UUID wordUuid;
    private UUID setUuid;
    private String word;

    private String translationUz;
    private String translationRu;
    private String translationTjk;
    private String translationKaa;
    private String translationKrg;

    private String definition;
    private String audioUrl;
    private String attachmentUrl;
    private String exampleSentence;
    private String transcription;
    private String partOfSpeech;
    private Integer coinReward;
    private Integer scoreReward;

}
