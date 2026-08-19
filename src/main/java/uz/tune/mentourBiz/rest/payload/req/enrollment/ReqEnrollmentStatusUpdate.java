package uz.tune.mentourBiz.rest.payload.req.enrollment;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;

@Getter
@Setter
public class ReqEnrollmentStatusUpdate {
    private EnrollmentStatus status;
}