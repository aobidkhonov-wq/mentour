package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.SchoolExamSettings;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.Map;

@Data
public class ResSchoolExamSettings {
    private boolean separateSection;
    private boolean noScreenshot;
    private int attemptLimit;
    private int timeLimit;
    private Map<LessonSectionType, Integer> sectionTimeLimits;
    private boolean freezeScreen;
    private int freezeTimer;

    public ResSchoolExamSettings(SchoolExamSettings settings) {
        this.separateSection = settings.isSeparateSection();
        this.noScreenshot = settings.isNoScreenshot();
        this.attemptLimit = settings.getAttemptLimit();
        this.timeLimit = settings.getTimeLimit();
        this.sectionTimeLimits = settings.getSectionTimeLimits();
        this.freezeScreen = settings.isFreezeScreen();
        this.freezeTimer = settings.getFreezeTimer();
    }
}