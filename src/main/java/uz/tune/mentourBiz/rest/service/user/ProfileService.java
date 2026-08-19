package uz.tune.mentourBiz.rest.service.user;

import uz.tune.mentourBiz.rest.payload.req.profile.ReqProfileChangePassword;
import uz.tune.mentourBiz.rest.payload.req.profile.ReqProfileUpdateInfo;
import uz.tune.mentourBiz.rest.payload.res.profile.ResProfileInfo;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.profile.ResStudentProfileInfo;


public interface ProfileService {
    ResProfileInfo getProfileInfo();
    ResponseMessage updateProfileInfo(ReqProfileUpdateInfo request);
    ResponseMessage changePassword(ReqProfileChangePassword request);
    ResStudentProfileInfo getStudentProfileInfo();
}