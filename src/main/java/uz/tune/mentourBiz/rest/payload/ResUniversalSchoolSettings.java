package uz.tune.mentourBiz.rest.payload;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.res.ResAcademicConfig;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolRewardConfig;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscription;

@Data
public class ResUniversalSchoolSettings {
    private ResAcademicConfig academic;
    private ResSchoolExamSettings exam;
    private ResSchoolRewardConfig reward;
    private ResSchoolSubscription subscription;
}