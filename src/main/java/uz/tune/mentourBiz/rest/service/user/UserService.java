package uz.tune.mentourBiz.rest.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.req.student.ReqReferralRegister;
import uz.tune.mentourBiz.rest.payload.req.user.ReqAdminResetPassword;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserCreate;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserDeleteList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserFreezeList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserUpdateDetails;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.user.ResUserInfo;

import java.util.UUID;

public interface UserService {
    User createUserAccount(String firstName, String lastName, String username, String password, UserRole role,String phoneNumber);

    boolean calculateIsAtRisk(Student student, SchoolAcademicConfig config);

    ResUserInfo getUserInfo();
    User getCurrentUser();
    ResUserInfo getUserByUuid(UUID userId);
    ResponseMessage createUser(ReqUserCreate request);
    ResponseMessage updateUser(UUID userId, ReqUserUpdateDetails request);
    ResponseMessage resetPassword(UUID userId, ReqAdminResetPassword request);
    ResponseMessage deleteUser(UUID userId, String note);
    ResponseMessage deleteUsers(ReqUserDeleteList request);
    ResponseMessage freezeUsers(ReqUserFreezeList request);
    Page<ResUserInfo> getAllUsers(UserStatus status, UserRole role, String fullName,
                                  String username, String schoolName, Pageable pageable,
                                  String schoolUuid, UUID classId, UUID courseId);
    ResponseMessage undeleteUser(UUID userId);
    ResponseMessage hardDeleteUser(UUID userId);
    ResponseMessage registerStudentViaReferral(ReqReferralRegister request);
    void validateStudentLimit(UUID schoolUuid, int newStudentsCount);
}