package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResLearningSummary {

    private long lessonsCompleted;
    private long unitsCompleted;
    private double homeworkCompletionRate;
    private double averageHomeworkGrade;

}
