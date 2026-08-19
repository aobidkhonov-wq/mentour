package uz.tune.mentourBiz.rest.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.domain.ReqOrganization;
import uz.tune.mentourBiz.rest.payload.ResOrganization;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.res.ResAcademicConfig;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolRewardConfig;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.UUID;

public interface OrganizationService {
    Page<ResOrganization> getAll(Pageable pageable);
    ResOrganization getOne(UUID uuid);
    ResponseMessage create(ReqOrganization req);
    ResponseMessage update(UUID uuid, ReqOrganization req);
    ResponseMessage delete(UUID uuid);
    ResponseMessage updateOrganizationAcademicConfig(UUID organizationUuid, ResAcademicConfig req);
    ResponseMessage updateOrganizationExamSettings(UUID orgaanizationUuid, ReqSchoolExamSettings req);
    ResponseMessage updateOrganizationRewardConfig(UUID organizationUuid, ResSchoolRewardConfig req);
}
