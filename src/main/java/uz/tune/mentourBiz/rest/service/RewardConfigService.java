package uz.tune.mentourBiz.rest.service;

import uz.tune.mentourBiz.rest.payload.res.ResSchoolRewardConfig;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import java.util.UUID;

public interface RewardConfigService {
    ResSchoolRewardConfig getConfig(UUID schoolUuid);
    ResponseMessage updateConfig(UUID schoolUuid, ResSchoolRewardConfig req);
}