package uz.tune.mentourBiz.rest.service.school;

import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.payload.req.attempt.ReqResetAttempt;
import uz.tune.mentourBiz.rest.payload.req.attempt.ReqSetAttemptLimit;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.attempt.ResAttemptStatus;
import uz.tune.mentourBiz.rest.payload.res.attempt.SchoolLimitAttempts;

import java.util.UUID;

public interface SchoolAttemptService {

    SchoolLimitAttempts getSchoolAttemptLimit(UUID schoolUuid);

    ResponseMessage setSchoolAttemptLimit(ReqSetAttemptLimit request);

    ResAttemptStatus getStatus(UUID taskUuid, Student student);

    ResAttemptStatus consumeAttempt(UUID taskUuid);

    ResAttemptStatus resetAttempt(ReqResetAttempt request);
}
