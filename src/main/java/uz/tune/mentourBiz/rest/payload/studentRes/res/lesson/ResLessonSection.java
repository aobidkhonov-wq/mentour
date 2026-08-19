package uz.tune.mentourBiz.rest.payload.studentRes.res.lesson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResLessonSection {
    private LessonSectionType type;
    private String title;
    private Integer progressPercentage;
    private boolean isLocked;
    private Integer remainingSeconds;

    public ResLessonSection(LessonSectionType type, String title, Integer progressPercentage, boolean isLocked) {
        this.type = type;
        this.title = title;
        this.progressPercentage = progressPercentage;
        this.isLocked = isLocked;
        this.remainingSeconds = null;
    }
}