package uz.tune.mentourBiz.rest.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.req.student.ReqCreateStudents;
import uz.tune.mentourBiz.rest.payload.req.student.ReqStudentAssign;
import uz.tune.mentourBiz.rest.payload.req.student.ReqStudentCreate;
import uz.tune.mentourBiz.rest.payload.req.student.ReqStudentsUpdate;
import uz.tune.mentourBiz.rest.payload.req.student.ReqUnassignedStudentAssign;
import uz.tune.mentourBiz.rest.payload.req.user.ReqApproveUserList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqDeclineUserList;
import uz.tune.mentourBiz.rest.payload.res.ResAtRiskStudent;
import uz.tune.mentourBiz.rest.payload.res.ResFinanceHistoryWrapper;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.student.ResBulkStudentResult;
import uz.tune.mentourBiz.rest.payload.res.studentApp.ResStudentHomeProfile;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentForLesson;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentList;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentOne;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StudentService {

    ResStudentOne getStudentByUuid(UUID uuid);
    List<ResStudentList> getAllStudents(UserStatus status);
    ResFinanceHistoryWrapper getStudentsBalanceHistory(UUID studentUuid, Pageable pageable, FinanceEnums.PaymentMethod method, Instant from, Instant to);

    ResFinanceHistoryWrapper getStudentsHistoryByType(UUID userUuid, FinanceEnums.FinanceTransactionType type, FinanceEnums.PaymentMethod method, Pageable pageable, Instant from, Instant to);
    Page<ResStudentList> getAllStudents(Integer page, Integer size, UserStatus status, String fullName,
                                        UUID schoolUuid, UUID classId, UUID courseId);
    ResponseMessage createStudents(ReqStudentCreate reqStudentCreate);
    ResBulkStudentResult createStudentsForGroup(
            UUID schoolId, List<uz.tune.mentourBiz.rest.payload.req.student.ReqBulkStudent> requests);
    ResponseMessage updateStudent(UUID uuid, ReqStudentsUpdate reqStudentsUpdate);
    ResponseMessage deleteStudent(UUID userUuid, String note);

//    Page<ResAtRiskStudent> getAtRiskStudents(UUID schoolUuid, Pageable pageable);

    List<ResStudentList> getSchoolStudentsToAddToClass(UUID groupUuid);
    List<ResStudentList> getStudentForClass(UUID groupId, String fullName);
    ResponseMessage transferStudent(UUID studentUuid, UUID targetGroupUuid, boolean withBillingPlan);
    ResponseMessage assignStudentsToClass(ReqStudentAssign reqStudentAssign);
    ResponseMessage assignUnassignedStudentsToGroup(ReqUnassignedStudentAssign req);
    ResponseMessage deAssignStudentFromClass(ReqStudentAssign reqStudentAssign);
    List<ResStudentForLesson> getStudentsForLesson(UUID lessonId);
    List<ResStudentList> getReferredStudents(UUID classId);
    ResponseMessage approveStudents(ReqApproveUserList reqApproveUserList);
    ResponseMessage declineStudents(ReqDeclineUserList reqDeclineUserList);
    ResponseMessage deReferStudent(ReqStudentAssign reqStudentAssign);

    Page<ResAtRiskStudent> getAtRiskStudents(UUID schoolUuid, Pageable pageable);
    ResStudentHomeProfile getStudentHomeProfile();
}

