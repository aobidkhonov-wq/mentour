package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResExerciseSection;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResUnitTaskInfos {
    private List<ResExerciseSection> sections;
}