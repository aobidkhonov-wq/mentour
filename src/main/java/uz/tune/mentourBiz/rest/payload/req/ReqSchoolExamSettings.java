package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.Map;

@Data
public class ReqSchoolExamSettings {
    private boolean separateSection;
    private boolean noScreenshot;
    private int attemptLimit;
    private int timeLimit;
    private Map<LessonSectionType, Integer> sectionTimeLimits;
    private boolean freezeScreen;
    private int freezeTimer;
}