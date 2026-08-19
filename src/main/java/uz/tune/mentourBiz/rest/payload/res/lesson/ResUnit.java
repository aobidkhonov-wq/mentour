package uz.tune.mentourBiz.rest.payload.res.lesson;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.SchoolExamSettings;
import uz.tune.mentourBiz.rest.domain.UnitExamSession;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.enums.UnitStatus;
import uz.tune.mentourBiz.rest.enums.UnitType;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ResUnit {
    private UUID id;
    private String title;
    private String topic;
    private Integer sortOrder;
    private String levelName;
    private UnitStatus status;
    private boolean isAdditional;
    private UnitType unitType;
    private Map<String, Object> examPolicy;
    private Integer minScoreToPass;
    private boolean aiExplanationEnabled;

    public ResUnit(Unit unit) {
        this.id = unit.getUuid();
        this.title = unit.getTitle();
        this.topic = unit.getTopic();
        this.status = unit.getStatus();
        this.sortOrder = unit.getSortOrder();
        this.unitType = unit.getUnitType();
        if (unit.getSchoolBook() != null) {
            this.isAdditional = !unit.getSchoolBook().isGlobal();
            if (unit.getSchoolBook().getLevel() != null) {
                this.levelName = unit.getSchoolBook().getLevel().getName();
            }
        }
    }

    public ResUnit(Unit unit, SchoolExamSettings settings, UnitExamSession session, SchoolAcademicConfig academicConfig, boolean aiEnabled) {
        this(unit);
        this.aiExplanationEnabled = aiEnabled;
        if (academicConfig != null) {
            this.minScoreToPass = academicConfig.getMinScoreToPass();
        }
        if (unit.getUnitType() == UnitType.EXAM && settings != null) {
            this.examPolicy = new HashMap<>();
            this.examPolicy.put("noScreenshot", settings.isNoScreenshot());
            this.examPolicy.put("freezeScreen", settings.isFreezeScreen());
            this.examPolicy.put("separateSection", settings.isSeparateSection());
            this.examPolicy.put("timeLimit", settings.getTimeLimit());
            this.examPolicy.put("attemptLimit", settings.getAttemptLimit());

            boolean isStarted = (session != null);
            this.examPolicy.put("isStarted", isStarted);

            if (isStarted && !settings.isSeparateSection() && !session.isFinished()) {
                long spent = Duration.between(session.getStartTime(), Instant.now()).getSeconds();
                int totalLimitSec = settings.getTimeLimit() * 60;
                this.examPolicy.put("globalRemainingSeconds", Math.max(0, totalLimitSec - (int) spent));
            }
        }
    }
}