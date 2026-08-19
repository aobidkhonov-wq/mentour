package uz.tune.mentourBiz.rest.service.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.req.school.ReqSchoolCreate;
import uz.tune.mentourBiz.rest.payload.req.school.ReqSchoolUpdate;
import uz.tune.mentourBiz.rest.payload.req.school.ReqUpdateMentorHours;
import uz.tune.mentourBiz.rest.payload.res.ResBookCoinStats;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.ResMentorsForSchool;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.util.List;
import java.util.UUID;

public interface SchoolService {
    Page<ResSchoolInfo> getAllSchools(Pageable pageable, SchoolStatus schoolStatus, String schoolName);
    List<ResSchoolInfo> getAllSchoolsList();
    ResSchoolInfo getSchoolById(UUID schoolId);
    ResponseMessage createSchool(ReqSchoolCreate request);
    ResponseMessage updateSchool(UUID schooldId, ReqSchoolUpdate request);
    ResponseMessage deleteSchool(UUID schoolId);
    Page<ResMentorsForSchool> getMentorsForSchool(UUID schoolId, Pageable pageable);
    ResponseMessage updateMentorHours(UUID schoolMentorId, ReqUpdateMentorHours request);
    ResponseMessage removeMentorFromSchool(UUID schoolMentorId);
    ResponseMessage hardDeleteSchool(UUID schoolId);
    ResponseMessage undeleteSchool(UUID schoolId);
    ResSchoolExamSettings getExamSettings(UUID schoolId);
    List<ResBookCoinStats> getBookCoinStats(UUID bookUuid);
    ResponseMessage updateExamSettings(UUID schoolId, ReqSchoolExamSettings request);
    List<ResSchoolInfo> getDirectorSchools();
    ResponseMessage backfillDefaults();

}