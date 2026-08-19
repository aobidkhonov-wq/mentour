package uz.tune.mentourBiz.rest.admin.req.upd;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import uz.tune.mentourBiz.qs.CircleContent;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.PronunciationContent;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.*;

@Data
public class ReqUpdateQuestion {
    private String instruction;
    private Integer coinReward;
    private Integer scoreReward;

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type",
            visible = true,
            defaultImpl = QuestionContent.class
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = GapFillContent.class, name = "GAP_FILL"),
            @JsonSubTypes.Type(value = SelectionContent.class, name = "SELECTION"),
            @JsonSubTypes.Type(value = OrderingContent.class, name = "ORDERING"),
            @JsonSubTypes.Type(value = MatchingContent.class, name = "MATCHING"),
            @JsonSubTypes.Type(value = WritingContent.class, name = "WRITING"),
            @JsonSubTypes.Type(value = SpeakingContent.class, name = "SPEAKING"),
            @JsonSubTypes.Type(value = PronunciationContent.class, name = "PRONUNCIATION"),
            @JsonSubTypes.Type(value = MultiSelectContent.class, name = "MULTI_SELECT"),
            @JsonSubTypes.Type(value = CircleContent.class, name = "CIRCLE"),
            @JsonSubTypes.Type(value = FixingContent.class, name = "FIXING_ANSWER"),
            @JsonSubTypes.Type(value = CircleTracingContent.class, name = "TRACING")
    })
    private QuestionContent content;

    private AnswerKey answerKey;
}
