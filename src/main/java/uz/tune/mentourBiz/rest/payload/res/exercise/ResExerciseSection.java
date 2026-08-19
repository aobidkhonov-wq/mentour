package uz.tune.mentourBiz.rest.payload.res.exercise;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResUnitTaskInfo;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResExerciseSection {
    private LessonSectionType type;
    private List<ResUnitTaskInfo> tasks;
}