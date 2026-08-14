package uz.tune.mentourBiz.rest.payload.req.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.DemoAttendanceStatus;

@Getter
@Setter
public class ReqMarkDemoAttendance {

    @NotNull
    private DemoAttendanceStatus status;

    private String note;
}
