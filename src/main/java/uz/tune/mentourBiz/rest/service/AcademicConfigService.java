package uz.tune.mentourBiz.rest.service;

import uz.tune.mentourBiz.rest.payload.req.ReqAtRiskThreshold;
import uz.tune.mentourBiz.rest.payload.res.ResAcademicConfig;
import uz.tune.mentourBiz.rest.payload.res.ResAtRiskThreshold;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.UUID;

public interface AcademicConfigService {
    ResAcademicConfig getConfig(UUID schoolUuid);
    ResponseMessage updateConfig(UUID schoolUuid, ResAcademicConfig req);

    // At-risk thresholds (attendance + result)
    ResAtRiskThreshold getAtRiskThresholds(UUID schoolUuid);
    ResponseMessage updateAtRiskThresholds(ReqAtRiskThreshold req);
}