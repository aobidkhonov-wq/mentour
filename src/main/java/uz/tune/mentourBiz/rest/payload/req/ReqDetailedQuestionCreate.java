package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.enums.GapFillMode;
import java.util.List;
import java.util.UUID;

@Data
public class ReqDetailedQuestionCreate {
    private UUID taskUuid;
    private ExerciseType type;
    private Integer coinReward;
    private Integer scoreReward;
    private String instruction;
    private Integer minWords;
    private Long secondsLimit;

    // Existing / Base fields
    private String textWithPlaceholders;
    private List<GapDef> gapDefinitions;
    private String fixingWrongText;
    private String fixingCorrectText;
    private String prompt;
    private String attachmentUrl;
    private uz.tune.mentourBiz.rest.enums.AttachmentMediaType attachmentMediaType;

    // --- NEW FIELDS FOR INTERACTIVE TYPES ---

    // Selection & Multi-Select & Circle
    private String questionText;
    private List<OptionDef> options;

    // Matching
    private List<ItemDef> leftItems;
    private List<ItemDef> rightItems;

    // Circle & Tracing
    private String targetWord;
    private List<CircleWordDef> circleParts;

    @Data
    public static class OptionDef {
        private String id;
        private String value;
        private String image; // Optional image for selection/multi
        private boolean isCorrect;
    }

    @Data
    public static class ItemDef {
        private String id;
        private String value;
        private String image;
    }

    @Data
    public static class CircleWordDef {
        private String wordId;
        private boolean isSpace;
        private List<OptionDef> chars;
    }

    @Data
    public static class GapDef {
        private String id;
        private GapFillMode mode;
        private List<String> options;
        private String hint;
        private List<String> correctAnswers;
    }
}