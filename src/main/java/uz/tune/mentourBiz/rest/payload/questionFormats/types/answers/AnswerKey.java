package uz.tune.mentourBiz.rest.payload.questionFormats.types.answers;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import uz.tune.mentourBiz.qs.CircleKey;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.*;
import uz.tune.mentourBiz.rest.repository.TracingKey;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = AnswerKey.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GapFillKey.class, name = "GAP_FILL"),
        @JsonSubTypes.Type(value = SelectionKey.class, name = "SELECTION"),
        @JsonSubTypes.Type(value = OrderingKey.class, name = "ORDERING"),
        @JsonSubTypes.Type(value = MatchingKey.class, name = "MATCHING"),
        @JsonSubTypes.Type(value = AnswerKey.class, name = "WRITING"),
        @JsonSubTypes.Type(value = AnswerKey.class, name = "SPEAKING"),
        @JsonSubTypes.Type(value = MultiSelectKey.class, name = "MULTI_SELECT"),
        @JsonSubTypes.Type(value = FixingKey.class, name = "FIXING_ANSWER"),
        @JsonSubTypes.Type(value = CircleKey.class, name = "CIRCLE"),
        @JsonSubTypes.Type(value = TracingKey.class, name = "TRACING")
})
public class AnswerKey {
    private ExerciseType type;
}
