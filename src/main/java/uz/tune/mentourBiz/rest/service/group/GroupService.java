package uz.tune.mentourBiz.rest.service.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.GroupStatus;
import uz.tune.mentourBiz.rest.payload.res.school.group.enrollment.ResGroupStudent;
import uz.tune.mentourBiz.rest.payload.req.group.ReqGroupCreate;
import uz.tune.mentourBiz.rest.payload.req.group.ReqGroupUpdate;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqGroupSchedule;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.ResReferralLink;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroupBalance;

import java.util.List;
import java.util.UUID;

public interface GroupService {
    ResponseMessage createGroup(ReqGroupCreate request);
    ResponseMessage updateGroup(UUID groupId, ReqGroupUpdate request);
    ResponseMessage deleteGroup(UUID groupId);
    ResponseMessage unDeleteGroup(UUID groupId);
    ResGroup getGroup(UUID groupId);
    Page<ResGroup> getGroups(Pageable pageable, UUID schoolId, UUID branchId, GroupStatus status,
                             String className, String levelName, String teacherName);
    ResReferralLink generateReferralLink(UUID groupId);
    List<ResGroup> getGroupsBySchool(UUID schoolUuid);
    List<ResGroupBalance> getGroupBalancesBySchool(UUID schoolUuid, String groupName, String teacherName, Boolean onlyOverdue);
    Page<ResGroupStudent> getGroupStudents(Integer page,Integer size, UUID groupUuid, String studentName,
                                           EnrollmentStatus status, Boolean onlyOverdue, UUID packageId);
    ResponseMessage createSchedule(ReqGroupSchedule request);
}