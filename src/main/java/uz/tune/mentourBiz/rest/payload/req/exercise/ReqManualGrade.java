package uz.tune.mentourBiz.rest.payload.req.exercise;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqManualGrade {
    private Integer score;   // 0-100, required
    private Integer coins;   // optional override; falls back to the dynamic exercise reward
    private String comment;  // optional teacher note saved as the answer content
}
