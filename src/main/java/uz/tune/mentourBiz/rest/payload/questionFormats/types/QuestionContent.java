package uz.tune.mentourBiz.rest.payload.questionFormats.types;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import uz.tune.mentourBiz.qs.CircleContent;
import uz.tune.mentourBiz.rest.enums.AttachmentMediaType;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.*;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
public class QuestionContent {
    private ExerciseType type;
    private String instruction;
    private String example;
    private String questionContent;
    private String attachmentUrl;
    private AttachmentMediaType attachmentMediaType;
}
