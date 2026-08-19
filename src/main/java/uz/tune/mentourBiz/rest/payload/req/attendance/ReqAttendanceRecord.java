package uz.tune.mentourBiz.rest.payload.req.attendance;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;

import java.util.UUID;

@Getter
@Setter
public class ReqAttendanceRecord {
    private UUID studentId;
    private AttendanceStatus status;
}