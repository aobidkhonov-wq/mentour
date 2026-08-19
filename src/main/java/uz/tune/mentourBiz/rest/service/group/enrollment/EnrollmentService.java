package uz.tune.mentourBiz.rest.service.group.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqAssignBillingPlan;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqEnrollmentCreate;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqEnrollmentStatusUpdate;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.group.enrollment.ResEnrollment;

import java.util.UUID;

public interface EnrollmentService {
    ResponseMessage createEnrollments(ReqEnrollmentCreate request);

    ResponseMessage assignBillingPlan(UUID groupUuid, ReqAssignBillingPlan request);

    ResponseMessage updateEnrollmentStatus(UUID enrollmentId, ReqEnrollmentStatusUpdate request);
    Page<ResEnrollment> getEnrollments(Pageable pageable, UUID groupId, UUID studentId);

    ResponseMessage restoreLesson(UUID enrollmentId, UUID lessonId);
}