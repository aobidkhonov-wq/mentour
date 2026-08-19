package uz.tune.mentourBiz.rest.payload.studentReq.req.exercise;


import lombok.Data;
import uz.tune.mentourBiz.rest.enums.ExerciseType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ReqExerciseSubmit {

    private ExerciseType type;
    private UUID taskUuid;

    // WRITING
    private String writingText;

    // SELECTION
    private String selectedOptionId;

    // ORDERING (List of Strings: ["GioScotty", "is", "stunning"])
    // / can't do with UUID bcz the words might be similar I -> I
    private List<String> orderingAnswer;

    private List<String> multiSelectAnswer;
    private String fixingAnswer;


    // GAP_FILL (Map "1" -> "UK")
    private Map<String, String> answers;

    // MATCHING (Map LeftUUID -> RightUUID)
    private Map<String, String> matchingPairs;

    private Boolean isFrontendSuccess; // Fallback
    private Map<Integer, Boolean> tracingResults; // Granular per-letter result
}
