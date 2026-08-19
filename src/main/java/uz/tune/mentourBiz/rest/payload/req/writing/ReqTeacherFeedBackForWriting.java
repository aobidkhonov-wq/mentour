package uz.tune.mentourBiz.rest.payload.req.writing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqTeacherFeedBackForWriting {
    private String feedback;
    private Integer score;
    private Integer coins;
    private Integer grammarScore;
    private Integer vocabularyScore;
    private Integer coherenceScore;
}
