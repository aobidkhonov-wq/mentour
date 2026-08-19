package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.answers;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ResWritingPreviewAnswers {
    private UUID questionUuid;
    private String name = "WRITING";


    public ResWritingPreviewAnswers(UUID questionUuid) {
        this.questionUuid = questionUuid;
    }
}
