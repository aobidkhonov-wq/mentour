package uz.tune.mentourBiz.rest.admin.req.upd;

import lombok.Data;

@Data
public class ReqUpdateVocabWord {
    private String word;
    private String translationUz;
    private String translationRu;
    private String definition;
    private String audioUrl;
    private String attachmentUrl;
    private String exampleSentence;
    private String transcription;
    private String partOfSpeech;
    private String translationTjk;
    private String translationKaa;
}