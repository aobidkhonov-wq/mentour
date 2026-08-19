package uz.tune.mentourBiz.rest.payload.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqFeedBackForSpeaking {
    private String feedback;
    private Integer grammarScore;
    private Integer vocabularyScore;
    private Integer coherenceScore;
    private Integer coins;
}
