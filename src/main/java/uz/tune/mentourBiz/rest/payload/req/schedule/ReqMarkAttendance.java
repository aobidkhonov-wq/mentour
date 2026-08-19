package uz.tune.mentourBiz.rest.payload.req.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqMarkAttendance {
    @NotNull
    private AttendanceStatus status;
    private String note;
    private UUID lessonUuid;
    private List<UUID> studentUuids;

    // Optional: the teacher who actually conducted this lesson (a substitute). When set, the lesson is
    // credited to this teacher in payroll. When null, the lesson's existing conductor is left unchanged.
    private UUID conductedByTeacherUuid;
}