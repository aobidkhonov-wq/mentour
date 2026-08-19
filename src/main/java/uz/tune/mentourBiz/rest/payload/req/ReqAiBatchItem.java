package uz.tune.mentourBiz.rest.payload.req;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReqAiBatchItem {
    private String correlationId;
    private String type;
    private String instruction;
    private Object questionContent;
    private String exerciseType;
    private Object correctAnswer;
    private Object studentAnswer;
    private String complexityLevel;
}