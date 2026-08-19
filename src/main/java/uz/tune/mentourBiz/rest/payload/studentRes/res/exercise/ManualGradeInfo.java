package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualGradeInfo {
    private boolean isManually;
    private String updatedBy;
    private Integer score;
}
