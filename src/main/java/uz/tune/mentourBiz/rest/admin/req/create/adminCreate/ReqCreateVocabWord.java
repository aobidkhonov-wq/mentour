package uz.tune.mentourBiz.rest.admin.req.create.adminCreate;

import lombok.Data;
import java.util.UUID;

@Data
public class ReqCreateVocabWord {
    private UUID setUuid;

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
    private String translationKrg;

}